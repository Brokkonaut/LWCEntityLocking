/*
 * Copyright 2011 Tyler Blair. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package com.griefcraft.modules.admin;

import com.griefcraft.bukkit.EntityBlock;
import com.griefcraft.lwc.LWC;
import com.griefcraft.model.Protection;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCCommandEvent;
import com.griefcraft.sql.Database;
import com.griefcraft.sql.PhysDB;
import com.griefcraft.util.Colors;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;

public class AdminCleanup extends JavaModule {

    /**
     * The amount of protection block gets to batch at once
     */
    private static int BATCH_SIZE = 100;

    @Override
    public void onCommand(LWCCommandEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.hasFlag("a", "admin")) {
            return;
        }

        LWC lwc = event.getLWC();
        CommandSender sender = event.getSender();
        String[] args = event.getArgs();

        if (!args[0].equals("cleanup")) {
            return;
        }

        // we have the right command
        event.setCancelled(true);

        // if we shouldn't output
        boolean silent = false;

        if (args.length > 1 && args[1].equalsIgnoreCase("silent")) {
            silent = true;
        }

        lwc.sendLocale(sender, "protection.admin.cleanup.start", "count", lwc.getPhysicalDatabase().getProtectionCount());

        // do the work in a separate thread so we don't fully lock the server
        // new Thread(new Admin_Cleanup_Thread(lwc, sender)).start();
        Bukkit.getScheduler().runTaskAsynchronously(lwc.getPlugin(), new Admin_Cleanup_Thread(lwc, sender, silent));
    }

    /**
     * Class that handles cleaning up the LWC database usage: /lwc admin cleanup
     */
    private static class Admin_Cleanup_Thread implements Runnable {

        private LWC lwc;
        private CommandSender sender;
        private boolean silent;

        public Admin_Cleanup_Thread(LWC lwc, CommandSender sender, boolean silent) {
            this.lwc = lwc;
            this.sender = sender;
            this.silent = silent;
        }

        /**
         * Push removal changes to the database
         *
         * @param toRemove
         */
        private void push(PhysDB database, List<Integer> toRemove) throws SQLException {
            final StringBuilder builder = new StringBuilder();
            final int total = toRemove.size();
            int count = 0;

            // iterate over the items to remove
            Iterator<Integer> iter = toRemove.iterator();

            // the database prefix
            String prefix = lwc.getPhysicalDatabase().getPrefix();

            // create the statement to use
            Statement statement = database.getConnection().createStatement();

            while (iter.hasNext()) {
                int protectionId = iter.next();

                if (count % 100000 == 0) {
                    builder.append("DELETE FROM ").append(prefix).append("protections WHERE id IN (").append(protectionId);
                } else {
                    builder.append(",").append(protectionId);
                }

                if (count % 100000 == 99999 || count == (total - 1)) {
                    builder.append(")");
                    statement.executeUpdate(builder.toString());
                    builder.setLength(0);

                    sender.sendMessage(Colors.Green + "REMOVED " + (count + 1) + " / " + total);
                }

                count++;
            }

            statement.close();
        }

        public void run() {
            List<Integer> toRemove = new ArrayList<Integer>();
            int removed = 0;
            int percentChecked = 0;

            try {
                sender.sendMessage(Colors.Red + "Processing cleanup request now in a separate thread");

                // the list of protections work off of. We batch updates to the world
                // so we can more than 20 results/second.
                final List<Protection> protections = new ArrayList<Protection>(BATCH_SIZE);

                // TODO separate stream logic to somewhere else :)
                // Create a new database connection, we are just reading
                PhysDB database = new PhysDB();
                database.connect();
                database.load();

                // amount of protections
                int totalProtections = database.getProtectionCount();

                Statement resultStatement = database.getConnection().createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

                if (database.getType() == Database.Type.MySQL) {
                    resultStatement.setFetchSize(BATCH_SIZE);
                }

                String prefix = lwc.getPhysicalDatabase().getPrefix();
                ResultSet result = resultStatement.executeQuery("SELECT id, owner, type, x, y, z, data, blockId, world, password, date, last_accessed, x>>4 AS xshift4,z>>4 AS zshift4 FROM " + prefix + "protections ORDER BY xshift4,zshift4");
                int checked = 0;
                boolean hasMore = true;
                while (hasMore) {
                    while (hasMore && protections.size() < BATCH_SIZE) {
                        // Wait until we have BATCH_SIZE protections
                        if (result.next()) {
                            Protection tprotection = database.resolveProtection(result);
                            if (tprotection != null) {
                                protections.add(tprotection);
                            }
                        } else {
                            hasMore = false;
                        }
                    }

                    // Check the blocks
                    Future<ArrayList<Integer>> getBlocks = Bukkit.getScheduler().callSyncMethod(lwc.getPlugin(), new Callable<ArrayList<Integer>>() {
                        public ArrayList<Integer> call() throws Exception {
                            ArrayList<Integer> toRemove = null;
                            for (Protection protection : protections) {
                                protection.uncacheBlock();
                                Block block = protection.getBlock(); // load the block

                                if (protection.getBlockId() == EntityBlock.ENTITY_BLOCK_ID) {
                                    // entity cleanup?
                                } else {
                                    // remove protections not found in the world
                                    if (block == null || !lwc.isProtectable(block)) {
                                        if (toRemove == null) {
                                            toRemove = new ArrayList<Integer>();
                                        }
                                        toRemove.add(protection.getId());

                                        if (!silent) {
                                            lwc.sendLocale(sender, "protection.admin.cleanup.removednoexist", "protection", protection.toString());
                                        }
                                    } else if (protection.getBlockMaterial() != block.getType()) {
                                        protection.setBlockMaterial(block.getType());
                                        protection.save();
                                        lwc.log("Updating material to " + block.getType() + " for block at " + block.getX() + "," + block.getY() + "," + block.getZ());
                                    }
                                }
                            }

                            return toRemove;
                        }
                    });

                    // Get all of the blocks
                    ArrayList<Integer> newToRemove = getBlocks.get();
                    if (newToRemove != null) {
                        toRemove.addAll(newToRemove);
                        removed += newToRemove.size();
                    }
                    checked += protections.size();

                    // percentage dump
                    int percent = (int) ((((double) checked) / totalProtections) * 20);

                    if (percentChecked != percent) {
                        percentChecked = percent;
                        sender.sendMessage(Colors.Red + "Cleanup @ " + (percent * 5) + "% [ " + checked + "/" + totalProtections + " protections ] [ removed " + removed + " protections ]");
                    }

                    // Clear the protection set, we are done with them
                    protections.clear();
                }

                // close the sql statements
                result.close();
                resultStatement.close();

                // flush all of the queries
                push(database, toRemove);

                database.dispose();

                final int totalChecked = checked;
                final int totalRemoved = removed;

                Bukkit.getScheduler().runTask(lwc.getPlugin(), new Runnable() {
                    @Override
                    public void run() {
                        // clear cache because we removed protections directly from the database
                        sender.sendMessage("Resetting cache...");
                        lwc.getPhysicalDatabase().precache();

                        sender.sendMessage("Cleanup completed. Removed " + totalRemoved + " protections out of " + totalChecked + " checked protections.");
                    }
                });

            } catch (Exception e) { // database.connect() throws Exception
                sender.sendMessage("Exception caught during cleanup: " + e.getMessage());
                lwc.getPlugin().getLogger().log(Level.SEVERE, "Exception caught during cleanup", e);
            }
        }
    }
}

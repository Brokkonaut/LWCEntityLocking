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

import com.griefcraft.lwc.LWC;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCCommandEvent;
import com.griefcraft.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.command.CommandSender;

public class AdminTags extends JavaModule {

    @Override
    public void onCommand(LWCCommandEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.hasFlag("a", "admin")) {
            return;
        }

        CommandSender sender = event.getSender();
        String[] args = event.getArgs();

        if (!args[0].equals("tags")) {
            return;
        }

        // we have the right command
        event.setCancelled(true);

        if (args.length < 2) {
            LWC.getInstance().sendSimpleUsage(sender, "/lwc admin tags <block>");
            return;
        }

        String block = args[1];
        if (block.startsWith("#")) {
            block = block.substring(1);
            NamespacedKey blockKey = NamespacedKey.fromString(block);
            if (blockKey == null) {
                sender.sendMessage(Colors.Red + "Invalid Tag");
                return;
            }
            Tag<Material> blockTag = Bukkit.getServer().getTag(Tag.REGISTRY_BLOCKS, blockKey, Material.class);
            if (blockTag == null) {
                sender.sendMessage(Colors.Red + "Invalid Tag");
                return;
            }

            sender.sendMessage(" ");
            sender.sendMessage(Colors.Red + " ==== Blocks for #" + blockTag.getKey().asMinimalString() + " ==== ");
            for (Material mat : blockTag.getValues()) {
                sender.sendMessage(Colors.Green + mat.getKey().asMinimalString());
            }

            return;
        }
        NamespacedKey blockKey = NamespacedKey.fromString(block);
        if (blockKey == null) {
            sender.sendMessage(Colors.Red + "Invalid Block");
            return;
        }
        Material material = Registry.MATERIAL.get(blockKey);
        if (material == null || !material.isBlock()) {
            sender.sendMessage(Colors.Red + "Invalid Block");
            return;
        }
        sender.sendMessage(" ");
        sender.sendMessage(Colors.Red + " ==== Tags for " + blockKey.asMinimalString() + " ==== ");
        for (Tag<Material> tag : Bukkit.getServer().getTags(Tag.REGISTRY_BLOCKS, Material.class)) {
            if (tag.isTagged(material)) {
                sender.sendMessage(Colors.Green + "#" + tag.getKey().asMinimalString());
            }
        }
    }
}
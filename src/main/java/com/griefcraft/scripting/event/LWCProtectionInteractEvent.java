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

package com.griefcraft.scripting.event;

import com.griefcraft.model.Protection;
import com.griefcraft.scripting.Module;
import com.griefcraft.scripting.ModuleLoader;
import java.util.Set;
import org.bukkit.event.player.PlayerInteractEvent;

public class LWCProtectionInteractEvent extends LWCProtectionEvent implements IResult {

    private PlayerInteractEvent event;
    private Set<String> actions;
    private Module.Result result = Module.Result.DEFAULT;

    public LWCProtectionInteractEvent(PlayerInteractEvent event, Protection protection, Set<String> actions, boolean canAccess, boolean canAdmin) {
        super(ModuleLoader.Event.INTERACT_PROTECTION, event.getPlayer(), protection, canAccess, canAdmin);

        this.event = event;
        this.actions = actions;
    }

    /**
     * Check if the player's actions contains the action
     *
     * @param action
     * @return
     */
    public boolean hasAction(String action) {
        return actions.contains(action);
    }

    public PlayerInteractEvent getEvent() {
        return event;
    }

    public Set<String> getActions() {
        return actions;
    }

    @Override
    public Module.Result getResult() {
        return result;
    }

    @Override
    public void setResult(Module.Result result) {
        this.result = result;
    }

}

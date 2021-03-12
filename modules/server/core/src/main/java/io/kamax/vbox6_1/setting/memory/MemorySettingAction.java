/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2018 Kamax Sarl
 * 
 * http://kamax.io/hbox/
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package io.kamax.vbox6_1.setting.memory;

import io.kamax.hbox.constant.MachineAttribute;
import io.kamax.tools.logging.Logger;
import io.kamax.tools.setting._Setting;
import io.kamax.vbox.settings.memory.MemorySetting;
import io.kamax.vbox6_1.setting._MachineSettingAction;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.LockType;

public class MemorySettingAction implements _MachineSettingAction {

    @Override
    public LockType getLockType() {
        return LockType.Write;
    }

    @Override
    public String getSettingName() {
        return MachineAttribute.Memory.toString();
    }

    @Override
    public void set(IMachine machine, _Setting setting) {
        machine.setMemorySize(setting.getNumber());
        Logger.debug(machine.getId() + " changed RAM to " + setting.getValue().toString());
    }

    @Override
    public _Setting get(IMachine machine) {
        return new MemorySetting(machine.getMemorySize());
    }

}

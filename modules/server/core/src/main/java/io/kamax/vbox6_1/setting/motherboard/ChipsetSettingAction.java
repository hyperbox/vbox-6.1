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

package io.kamax.vbox6_1.setting.motherboard;

import io.kamax.hbox.constant.MachineAttribute;
import io.kamax.tools.setting.StringSetting;
import io.kamax.tools.setting._Setting;
import io.kamax.vbox.settings.motherboard.ChipsetSetting;
import io.kamax.vbox6_1.setting._MachineSettingAction;
import org.virtualbox_6_1.ChipsetType;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.LockType;

public class ChipsetSettingAction implements _MachineSettingAction {

    @Override
    public LockType getLockType() {
        return LockType.Write;
    }

    @Override
    public String getSettingName() {
        return MachineAttribute.Chipset.toString();
    }

    @Override
    public void set(IMachine machine, _Setting setting) {
        machine.setChipsetType(ChipsetType.valueOf(((StringSetting) setting).getValue()));
    }

    @Override
    public _Setting get(IMachine machine) {
        return new ChipsetSetting(machine.getChipsetType().toString());
    }

}

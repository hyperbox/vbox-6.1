/*
 * Hyperbox - Virtual Infrastructure Manager
 * Copyright (C) 2018 Kamax Sarl
 *
 * https://apps.kamax.io/hyperbox
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

package io.kamax.apps.hbox.vbox6_1.setting.machine;

import io.kamax.apps.hbox.vbox6_1.data.Mappings;
import io.kamax.apps.hbox.vbox6_1.setting._MachineSettingAction;
import io.kamax.hbox.constant.MachineAttribute;
import io.kamax.hbox.constant.MouseMode;
import io.kamax.hbox.hypervisor.vbox.settings.general.MouseModeSetting;
import io.kamax.tools.setting.StringSetting;
import io.kamax.tools.setting._Setting;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.LockType;
import org.virtualbox_6_1.PointingHIDType;

public class MouseModeSettingAction implements _MachineSettingAction {

    @Override
    public LockType getLockType() {
        return LockType.Write;
    }

    @Override
    public String getSettingName() {
        return MachineAttribute.MouseMode.toString();
    }

    @Override
    public void set(IMachine machine, _Setting setting) {
        PointingHIDType mouseMode = Mappings.get(MouseMode.valueOf(((StringSetting) setting).getValue()));
        machine.setPointingHIDType(mouseMode);
    }

    @Override
    public _Setting get(IMachine machine) {
        return new MouseModeSetting(Mappings.get(machine.getPointingHIDType()));
    }

}

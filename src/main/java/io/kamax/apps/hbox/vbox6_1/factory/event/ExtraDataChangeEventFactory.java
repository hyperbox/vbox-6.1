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

package io.kamax.apps.hbox.vbox6_1.factory.event;

import io.kamax.apps.hbox.vbox6_1.factory._PreciseEventFactory;
import io.kamax.hbox.event._Event;
import io.kamax.tools.logging.Logger;
import org.virtualbox_6_1.IEvent;
import org.virtualbox_6_1.IExtraDataChangedEvent;
import org.virtualbox_6_1.VBoxEventType;

public class ExtraDataChangeEventFactory implements _PreciseEventFactory {

    @Override
    public VBoxEventType getType() {
        return VBoxEventType.OnExtraDataChanged;
    }

    @Override
    public IEvent getRaw(IEvent vbEvent) {
        return IExtraDataChangedEvent.queryInterface(vbEvent);
    }

    @Override
    public _Event getEvent(IEvent vbEvent) {
        IExtraDataChangedEvent ev = (IExtraDataChangedEvent) vbEvent;
        Logger.debug("ExtraDataChanged event for " + (ev.getMachineId() == null ? "global data" : "Machine ID ") + ev.getMachineId() + " | Key: " + ev.getKey()
                + " | Value: " + ev.getValue());
        return null;
    }

}

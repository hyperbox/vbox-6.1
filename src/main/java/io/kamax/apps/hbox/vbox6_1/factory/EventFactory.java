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

package io.kamax.apps.hbox.vbox6_1.factory;

import io.kamax.hbox.ClassManager;
import io.kamax.hbox.event._Event;
import io.kamax.hbox.exception.HyperboxException;
import io.kamax.tools.logging.Logger;
import org.virtualbox_6_1.IEvent;
import org.virtualbox_6_1.VBoxEventType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EventFactory {

    private static Map<VBoxEventType, _PreciseEventFactory> factories;

    static {
        factories = new HashMap<VBoxEventType, _PreciseEventFactory>();

        try {
            Logger.debug("Current class loader: " + EventFactory.class.getClassLoader());
            Logger.debug("Interface class loader: " + _PreciseEventFactory.class.getClassLoader());
            Set<_PreciseEventFactory> factoriesSet = ClassManager.getAllOrFail(_PreciseEventFactory.class);
            for (_PreciseEventFactory factory : factoriesSet) {
                factories.put(factory.getType(), factory);
            }
        } catch (HyperboxException e) {
            throw new HyperboxException(e);
        }
    }

    public static IEvent getRaw(IEvent rawEvent) {
        if (factories.containsKey(rawEvent.getType())) {
            try {
                return factories.get(rawEvent.getType()).getRaw(rawEvent);
            } catch (Throwable t) {
                Logger.error("Unable to process event: " + t.getMessage());
                return null;
            }
        } else {
            Logger.debug("Unknown event : " + rawEvent.getType());
            return null;
        }
    }

    public static _Event get(IEvent rawEvent) {
        if (factories.containsKey(rawEvent.getType())) {
            try {
                return factories.get(rawEvent.getType()).getEvent(rawEvent);
            } catch (Throwable t) {
                Logger.error("Unable to process event: " + t.getMessage());
                return null;
            }
        } else {
            Logger.debug("Unknown event : " + rawEvent.getType());
            return null;
        }
    }

}

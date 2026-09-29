package com.chagui68.multiversenets.compat;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * [EN] Fixtures for {@link ProtectionBridge#findStatic} and
 * {@link ProtectionBridge#findMethod}.
 * <p>
 * The providers all bind a handful of methods by name and give up if the shape is not there. That
 * "give up" is the behaviour worth testing, because getting it wrong is what previously made
 * ProtectionStones look wired up while covering nothing: the provider accepted a method that threw
 * on every call. These fixtures are the shapes that must, and must not, be accepted.
 *
 * [ES] Fixtures para {@link ProtectionBridge#findStatic} y
 * {@link ProtectionBridge#findMethod}.
 * <p>
 * Todos los providers enlazan unos pocos métodos por nombre y renuncian si la forma no está. Ese
 * "renunciar" es justo lo que hay que testear, porque fallar ahí es lo que antes hacía que
 * ProtectionStones pareciera conectado sin cubrir nada. Estas fixtures son las formas que deben, y
 * no deben, aceptarse.
 */
final class ResolverFixtures {

    private ResolverFixtures() {
    }

    /** [EN] An older build that only ships the safe lookup. */
    static class LegacyOnly {
        public static Object fromLocation(Location location) {
            return null;
        }
    }

    /** [EN] A build where the method lost its {@code static} modifier. */
    static class InstanceOnly {
        public Object fromLocation(Location location) {
            return null;
        }
    }

    /** [EN] A build where the method changed its parameter list. */
    static class WrongParameters {
        public static Object fromLocation(World world, int x) {
            return null;
        }
    }

    /** [EN] An unrelated class, to prove a miss returns null instead of throwing. */
    static class Empty {
    }

    /** [EN] Both a static and an instance method with the same name and parameters. */
    static class Ambiguous {
        public static Object resolve(Location location) {
            return null;
        }

        public Object resolve2(Location location) {
            return null;
        }
    }

    /** [EN] A static method, present so {@code findMethod} has something to reject. */
    static class StaticIsEmpty {
        public static boolean isEmpty() {
            return true;
        }
    }

    /** [EN] The real shape: an instance method inherited from a supertype. */
    static class InstanceIsEmpty {
        public boolean isEmpty() {
            return true;
        }
    }
}

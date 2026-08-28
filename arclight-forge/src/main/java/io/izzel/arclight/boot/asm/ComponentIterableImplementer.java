package io.izzel.arclight.boot.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Grafts Spigot's {@code Iterable<Component>} contract onto {@code net.minecraft.network.chat.Component}.
 *
 * <p>CraftBukkit is compiled against Spigot's patched {@code Component}, so {@code CraftChatMessage} calls
 * {@code Component.iterator()} directly. Forge ships the vanilla interface without it.
 *
 * <p>This used to be an interface mixin, but Mixin instantiates every {@code IMixinConfigPlugin} and calls
 * {@code onLoad()} on all of them <em>before</em> it sorts configs by priority and prepares a single one.
 * A mod whose plugin touches {@code Component} on that first pass gets the class loaded untransformed, and
 * no config priority can change that. A launch plugin runs on every class load, so it has no such race.
 */
public class ComponentIterableImplementer implements Implementer {

    private static final String TARGET = "net/minecraft/network/chat/Component";
    private static final String ITERABLE = "java/lang/Iterable";
    private static final String HELPER = "io/izzel/arclight/common/mod/util/ComponentIterables";

    @Override
    public boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) {
        if (!TARGET.equals(node.name) || node.interfaces.contains(ITERABLE)) {
            return false;
        }
        node.interfaces.add(ITERABLE);
        delegate(node, "iterator", "()Ljava/util/Iterator;",
            "()Ljava/util/Iterator<L" + TARGET + ";>;");
        delegate(node, "bridge$iterator", "()Ljava/util/Iterator;",
            "()Ljava/util/Iterator<L" + TARGET + ";>;");
        delegate(node, "bridge$stream", "()Ljava/util/stream/Stream;",
            "()Ljava/util/stream/Stream<L" + TARGET + ";>;");
        ArclightImplementer.LOGGER.debug("Implemented Iterable on {}", node.name);
        return true;
    }

    /** Adds a default method whose whole body is {@code return Helper.<name>(this);}. */
    private static void delegate(ClassNode node, String name, String descriptor, String signature) {
        var method = new MethodNode(Opcodes.ACC_PUBLIC, name, descriptor, signature, null);
        var helperName = name.startsWith("bridge$") ? name.substring("bridge$".length()) : name;
        var insns = new InsnList();
        insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, helperName,
            "(L" + TARGET + ";)" + descriptor.substring(descriptor.indexOf(')') + 1), false));
        insns.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ARETURN));
        method.instructions = insns;
        method.maxStack = 1;
        method.maxLocals = 1;
        node.methods.add(method);
    }
}

package me.modmuss50.optium.patcher.fixes;


import org.objectweb.asm.tree.ClassNode;

public interface ClassFixer {
	void fix(ClassNode optifine, ClassNode minecraft);
}

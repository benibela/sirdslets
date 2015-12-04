

MKDIR = mkdir
#JAVAC = javac  -Xlint:deprecation -d output
JAVAC = javac  -d output

CLASSES = AbSIRDlet AccurateTiming Cuboid Floater FloatingObjectGroup IntArrayImage IntersectionUtils JSONReader JSONSerializable JSONWriter PrimitiveAnimator PrimitiveMarker PrimitiveModifier PrimitiveMover PrimitiveScaler RendererSoftware SceneManager SceneObjectGroup SceneObject ScenePrimitive SIRDSAppletManager SIRDSApplets SIRDSFlighterEditor SIRDSFlighter SIRDSlet SIRDSRenderer SIRDSxkcd SPonglet Translations_DE Translations Vector3d Vector3i ZDraw ZSprite ZSpriteRepeater

SUBCLASSES = $(CLASSES:%=output/%.class)

all: output $(SUBCLASSES)

output:
	$(MKDIR) output


output/%.class:%.java
	$(JAVAC) $<

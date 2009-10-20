

MKDIR = mkdir
#JAVAC = javac  -Xlint:deprecation -d output
JAVAC = javac  -d output

CLASSES = IntArrayImage ZDraw ZSprite Floater SIRDSlet SIRDSAppletManager SIRDSApplets AbSIRDlet SIRDSFlighter SIRDSFlighterEditor

SUBCLASSES = $(CLASSES:%=output/%.class)

all: output $(SUBCLASSES)

output:
	$(MKDIR) output


output/%.class:%.java
	$(JAVAC) $<

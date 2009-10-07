

MKDIR = mkdir
JAVAC = javac -d output

CLASSES = ZDraw Floater SIRDSlet SIRDSAppletManager SIRDSApplets AbSIRDlet

SUBCLASSES = $(CLASSES:%=output/%.class)

output:
	$(MKDIR) output

all: output $(SUBCLASSES)


output/%.class:%.java
	$(JAVAC) $<

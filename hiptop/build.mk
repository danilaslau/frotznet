
LIBS := $(SDKHOME)/libs
TOOLS := $(SDKHOME)/tools/linux
MD5SUM := md5sum
JFLAGS := -classpath $(LIBS)/library.jar:. -d classes

DRC := $(TOOLS)/drc 
MKBUNDLE ?= $(TOOLS)/mkbundle
JAVAC ?= javac
JAVA ?= java

APPNAME ?= app
BUNDLE := $(APPNAME).bndl

RSRC_SRC := $(APPNAME).rsrc
RSRC_DB := $(APPNAME).rdb

SRCS := $(shell find . -name \*.java)

STAMP := $(shell find . -name \*.java | $(MD5SUM) | sed 's/.\ .*//g')
STAMPFILE := stamp.$(STAMP)

all: $(STAMPFILE) $(APPNAME).bndl

$(STAMPFILE): $(SRCS) Resources.java Events.java $(RSRC_DB)
	@rm -rf classes stamp.* 
	@mkdir -p classes
	$(JAVAC) $(JFLAGS) $(SRCS)
	$(MKBUNDLE) -o classes/application.dat $(RSRC_DB)
	@touch $(STAMPFILE)

DRCOPTS := -S $(LIBS)/AppResources.java 

Resources.java Events.java: $(RSRC_SRC)
	$(DRC) $(DRCOPTS) -i $< -gr -ge

%.rdb: %.rsrc
	$(DRC) $(DRCOPTS) -i $< -o $@ 

SIMFLAGS := -Dcom.danger.autoboot=$(BUNDLENAME)

SIMCLASSPATH := -classpath $(LIBS)/simulator.jar:$(LIBS)/library.jar:classes

run:: run-color

run-color: $(STAMPFILE) 
	$(JAVA) -Dcom.danger.screen.color_space=color16 $(SIMFLAGS) $(SIMCLASSPATH) danger.Boot 

run-gray: $(STAMPFILE) 
	$(JAVA) -Dcom.danger.screen.color_space=gray $(SIMFLAGS) $(SIMCLASSPATH) danger.Boot 

%.bndl: $(STAMPFILE) $(RSRC_DB)
	$(MKBUNDLE) -o $@ $(LIBS)/library.link classes $(RSRC_DB) -l $(APPNAME).lst 

clean::
	rm -rf classes *~ *.rdb *.bndl *.lst stamp.* 

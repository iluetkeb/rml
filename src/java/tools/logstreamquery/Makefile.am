.PHONY: all

ASS_ARGS=-DassembleDirectory=@prefix@ -DrepositoryName=share/repository/

all:
	mvn compile
	mvn appassembler:assemble

clean:
	mvn clean

install: all
	mvn -DskipTests -Dmaven.repo.local=@prefix@/share/repository install
	mvn appassembler:assemble $(ASS_ARGS)
	for file in target/appassembler/bin/*; do f=$$(basename $$file); chmod 755 @bindir@/$$f; done


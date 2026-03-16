.PHONY: all clean shim lexer link

all: shim lexer link replay replay-shim

lexer:
	-polyc build.sml

shim:
	cc -c polylex_c_shim.c -o polylex_c_shim.o

link:
	afl-clang-fast -o polylex_fuzz polylex_fuzz.o polylex_c_shim.o \
  -L/usr/local/lib -Wl,-rpath,/usr/local/lib \
  -lpolymain -lpolyml

replay-shim:
	cc -c polylex_replay_shim.c -o polylex_replay_shim.o

replay: replay-shim
	cc -o  polylex_replay polylex_fuzz.o polylex_replay_shim.o \
		-L/usr/local/lib -Wl,-rpath,/usr/local/lib \
		-lpolymain -lpolyml


clean:
	rm -f polylex_fuzz polylex_fuzz.o polylex_c_shim.o
	rm -f polylex_replay polylex_replay_shim.o
	rm -rf coverage_out

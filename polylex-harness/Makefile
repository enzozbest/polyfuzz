.PHONY: all clean shim lexer link

all: shim lexer link

lexer:
	-polyc build.sml

shim:
	afl-clang-fast -c polylex_c_shim.c -o polylex_c_shim.o

link:
	afl-clang-fast -o polylex_fuzz polylex_fuzz.o polylex_c_shim.o \
  -L/usr/local/lib -Wl,-rpath,/usr/local/lib \
  -lpolymain -lpolyml

clean:
	rm -f polylex_fuzz polylex_fuzz.o polylex_c_shim.o

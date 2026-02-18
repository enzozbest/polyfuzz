.PHONY: all clean shim lexer link

all: shim lexer link

lexer:
	polyc build.sml

shim:
	afl-clang-fast -c polylex_c_shim.c -o polylex_c_shim.o

link:
	afl-clang-fast -o fuzz_polylex polylex_fuzz.o afl_shim.o \
  -L<polyml_lib_path> -Wl,-rpath,<polyml_lib_path> \
  -lpolymain -lpolyml

clean:
	rm polylex_fuzz polylex_fuzz.o polylex_c_shim.o
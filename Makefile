# PolyFuzz top-level Makefile
#
# Builds all four components in dependency order.
# NOTE: smlgen is a git submodule. Run `git submodule update --init smlgen`
# before building.

.PHONY: all build-smlgen build-polylex build-diffcomp build-orchestrator test check clean

all: build-smlgen build-polylex build-diffcomp build-orchestrator

build-smlgen:
	cd smlgen && ./gradlew installDist

build-polylex:
	$(MAKE) -C polylex-harness

build-diffcomp:
	cd diffcomp && ./gradlew installDist

build-orchestrator:
	cd orchestrator && uv sync

test:
	cd orchestrator && uv run pytest

check: all
	@echo "Verifying build artifacts..."
	@test -f smlgen/build/libs/smlgen.jar || (echo "ERROR: smlgen/build/libs/smlgen.jar not found" && exit 1)
	@test -x polylex-harness/polylex_fuzz || (echo "ERROR: polylex-harness/polylex_fuzz not found or not executable" && exit 1)
	@test -x diffcomp/build/install/diffcomp/bin/diffcomp || (echo "ERROR: diffcomp/build/install/diffcomp/bin/diffcomp not found or not executable" && exit 1)
	@echo "All artifacts verified."

clean:
	-cd smlgen && ./gradlew clean 2>/dev/null || true
	-$(MAKE) -C polylex-harness clean 2>/dev/null || true
	-cd diffcomp && ./gradlew clean 2>/dev/null || true

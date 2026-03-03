"""Deterministic seed derivation and master seed generation.

Provides pure functions for generating reproducible campaign seeds from a master seed.
Same master_seed + campaign_index always produces the same campaign_seed.
"""

from __future__ import annotations

import hashlib
import secrets


def generate_master_seed() -> int:
    """Generate a cryptographically random 63-bit positive integer.

    Used as the master seed for an experiment.
    """
    return secrets.randbelow(2**63)


def derive_campaign_seed(master_seed: int, campaign_index: int) -> int:
    """Derive a deterministic campaign seed from a master seed and index.

    This is a PURE function: same inputs always produce the same output.
    Uses SHA-256 to derive a 63-bit positive integer from the concatenation
    of master_seed bytes (8 bytes, big-endian) and campaign_index bytes
    (4 bytes, big-endian).
    Args:
        master_seed: The experiment-level master seed.
        campaign_index: Zero-based campaign index.
    Returns:
        A deterministic 63-bit positive integer seed for the campaign.
    """
    data = master_seed.to_bytes(8, byteorder="big") + campaign_index.to_bytes(
        4, byteorder="big"
    )
    digest = hashlib.sha256(data).digest()
    return int.from_bytes(digest[:8], byteorder="big") & ((1 << 63) - 1)

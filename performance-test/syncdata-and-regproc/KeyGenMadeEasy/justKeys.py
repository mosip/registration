from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives import serialization
import os

OUTPUT_DIR = "generated_keys"
NUM_KEYS = 5000   # change to 5000 if you need that many

os.makedirs(OUTPUT_DIR, exist_ok=True)

for i in range(1, NUM_KEYS + 1):
    # Generate private key
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048
    )

    # ---- Save private key in PKCS#8 DER (.key) ----
    priv_bytes = private_key.private_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PrivateFormat.PKCS8,
        encryption_algorithm=serialization.NoEncryption()
    )
    with open(os.path.join(OUTPUT_DIR, f"reg_{i}.key"), "wb") as f:
        f.write(priv_bytes)

    # ---- Save public key in X.509 SubjectPublicKeyInfo DER (.pub) ----
    pub_bytes = private_key.public_key().public_bytes(
        encoding=serialization.Encoding.DER,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    with open(os.path.join(OUTPUT_DIR, f"reg_{i}.pub"), "wb") as f:
        f.write(pub_bytes)

    # ---- Also save PEM (for readability) ----
    pem_bytes = private_key.public_key().public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    with open(os.path.join(OUTPUT_DIR, f"reg_{i}.pem"), "wb") as f:
        f.write(pem_bytes)

    if i % 100 == 0:
        print(f"✅ Generated {i} key pairs")


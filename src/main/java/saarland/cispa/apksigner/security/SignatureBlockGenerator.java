/*
 * Copyright (C) 2010 Ken Ellinwood
 *
 * Changes Copyright (C) 2017 CISPA (https://cispa.saarland), Saarland University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package saarland.cispa.apksigner.security;

import org.spongycastle.cert.jcajce.JcaCertStore;
import org.spongycastle.cms.CMSProcessableByteArray;
import org.spongycastle.cms.CMSSignedData;
import org.spongycastle.cms.CMSSignedDataGenerator;
import org.spongycastle.cms.CMSTypedData;
import org.spongycastle.cms.SignerInfoGenerator;
import org.spongycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.DigestCalculatorProvider;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;
import org.spongycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.spongycastle.util.Store;

import java.util.ArrayList;
import java.util.List;

import saarland.cispa.apksigner.sign.KeySet;

/**
 *
 */
public class SignatureBlockGenerator {

    /**
     * Sign the given content using the private and public keys from the keySet, and return the encoded CMS (PKCS#7) data.
     * Use of direct signature and DER encoding produces a block that is verifiable by Android recovery programs.
     */
    public static byte[] generate(KeySet keySet, byte[] content) {
        try {
            List certList = new ArrayList();
            CMSTypedData msg = new CMSProcessableByteArray(content);

            certList.add(keySet.getPublicKey());

            Store certs = new JcaCertStore(certList);

            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();

            JcaContentSignerBuilder jcaContentSignerBuilder = new JcaContentSignerBuilder(keySet.getSignatureAlgorithm()).setProvider("SC");
            ContentSigner sha1Signer = jcaContentSignerBuilder.build(keySet.getPrivateKey());

            JcaDigestCalculatorProviderBuilder jcaDigestCalculatorProviderBuilder = new JcaDigestCalculatorProviderBuilder().setProvider("SC");
            DigestCalculatorProvider digestCalculatorProvider = jcaDigestCalculatorProviderBuilder.build();

            JcaSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder( digestCalculatorProvider);
            jcaSignerInfoGeneratorBuilder.setDirectSignature(true);
            SignerInfoGenerator signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(sha1Signer, keySet.getPublicKey());

            gen.addSignerInfoGenerator( signerInfoGenerator);

            gen.addCertificates(certs);

            CMSSignedData sigData = gen.generate(msg, false);
            return sigData.toASN1Structure().getEncoded("DER");

        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

}

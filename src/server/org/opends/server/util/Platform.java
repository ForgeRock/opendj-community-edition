/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2013-2015 ForgeRock AS.
 */

package org.opends.server.util;



import java.security.KeyStoreException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.opends.messages.Message;
import static org.opends.messages.UtilityMessages.*;



/**
 * Provides a wrapper class that collects all of the JVM vendor and JDK version
 * specific code in a single place.
 */
public final class Platform
{

  // Prefix that determines which security package to use.
  private static final String pkgPrefix;

  // The two security package prefixes (IBM and SUN).
  private static final String IBM_SEC = "com.ibm.security";
  private static final String SUN_SEC = "sun.security";

  /** System property to use for overriding CertAndKeyGen class location. */
  public static final String CERTANDKEYGEN_PROVIDER = "org.forgerock.opendj.CertAndKeyGenProvider";

  /** The CertAndKeyGen class is located in different packages depending on JVM environment. */
  private static final String[] CERTANDKEYGEN_PATHS = new String[] {
      "sun.security.x509.CertAndKeyGen",          // Oracle/Sun/OpenJDK 6,7
      "sun.security.tools.keytool.CertAndKeyGen", // Oracle/Sun/OpenJDK 8
      "com.ibm.security.x509.CertAndKeyGen",      // IBM SDK 7
      "com.ibm.security.tools.CertAndKeyGen"      // IBM SDK 8
  };

  private static final PlatformIMPL IMPL;

  static
  {
    String vendor = System.getProperty("java.vendor");

    if (vendor.startsWith("IBM"))
    {
      pkgPrefix = IBM_SEC;
    }
    else
    {
      pkgPrefix = SUN_SEC;
    }
    IMPL = new DefaultPlatformIMPL();
  }



  /**
   * Platform base class. Performs all of the certificate management functions.
   */
  private abstract static class PlatformIMPL
  {

    // Key size, key algorithm and signature algorithms used.
    private static final int KEY_SIZE = 2048;
    private static final String KEY_ALGORITHM = "rsa";
    private static final String SIG_ALGORITHM = "SHA256WithRSA";

    // Time values used in validity calculations.
    private static final int SEC_IN_DAY = 24 * 60 * 60;

    // Methods pulled from the classes.
    private static final String GENERATE_METHOD = "generate";
    private static final String GET_PRIVATE_KEY_METHOD = "getPrivateKey";
    private static final String GET_SELFSIGNED_CERT_METHOD =
      "getSelfCertificate";

    // Classes needed to manage certificates.
    private static final Class<?> certKeyGenClass, X500NameClass;

    // Constructors for each of the above classes.
    private static Constructor<?> certKeyGenCons, X500NameCons;

    static
    {
      String certAndKeyGen = getCertAndKeyGenClassName();
      if(certAndKeyGen == null)
      {
        Message msg = ERR_CERTMGR_CERTGEN_NOT_FOUND.get(CERTANDKEYGEN_PROVIDER);
        throw new ExceptionInInitializerError(msg.toString());
      }

      String X500Name = pkgPrefix + ".x509.X500Name";
      try
      {
        certKeyGenClass = Class.forName(certAndKeyGen);
        X500NameClass = Class.forName(X500Name);
        certKeyGenCons = certKeyGenClass.getConstructor(String.class,
            String.class);
        X500NameCons = X500NameClass.getConstructor(String.class);
      }
      catch (ClassNotFoundException e)
      {
        Message msg = ERR_CERTMGR_CLASS_NOT_FOUND.get(e.getMessage());
        throw new ExceptionInInitializerError(msg.toString());
      }
      catch (SecurityException e)
      {
        Message msg = ERR_CERTMGR_SECURITY.get(e.getMessage());
        throw new ExceptionInInitializerError(msg.toString());
      }
      catch (NoSuchMethodException e)
      {
        Message msg = ERR_CERTMGR_NO_METHOD.get(e.getMessage());
        throw new ExceptionInInitializerError(msg.toString());
      }
    }

    /**
     * Try to decide which CertAndKeyGen class to use.
     *
     * @return a fully qualified class name or null
     */
    private static String getCertAndKeyGenClassName() {
      String certAndKeyGen = System.getProperty(CERTANDKEYGEN_PROVIDER);
      if (certAndKeyGen != null)
      {
        return certAndKeyGen;
      }

      for (String className : CERTANDKEYGEN_PATHS)
      {
        if (classExists(className))
        {
          return className;
        }
      }
      return null;
    }

    /**
     * A quick check to see if a class can be loaded. Doesn't check if
     * it can be instantiated.
     *
     * @param className full class name to check
     * @return true if the class is found
     */
    private static boolean classExists(final String className)
    {
      try {
        Class clazz = Class.forName(className);
        return true;
      } catch (ClassNotFoundException e) {
        return false;
      } catch (ClassCastException e) {
        return false;
      }
    }

    protected PlatformIMPL()
    {
    }



    /**
     * Delete the specified alias from the specified keystore.
     *
     * @param ks
     *          The keystore to delete the alias from.
     * @param ksPath
     *          The path to the keystore.
     * @param alias
     *          The alias to use in the request generation.
     * @param pwd
     *          The keystore password to use.
     * @throws KeyStoreException
     *           If an error occurred deleting the alias.
     */
    public final void deleteAlias(KeyStore ks, String ksPath, String alias,
        char[] pwd) throws KeyStoreException
    {
      try
      {
        if (ks == null)
        {
          Message msg = ERR_CERTMGR_KEYSTORE_NONEXISTANT.get();
          throw new KeyStoreException(msg.toString());
        }
        ks.deleteEntry(alias);
        FileOutputStream fs = new FileOutputStream(ksPath);
        ks.store(fs, pwd);
        fs.close();
      }
      catch (Exception e)
      {
        Message msg = ERR_CERTMGR_DELETE_ALIAS.get(alias, e.getMessage());
        throw new KeyStoreException(msg.toString());
      }
    }



    /**
     * Add the certificate in the specified path to the specified keystore,
     * creating the keystore using the specified type and path if it the
     * keystore doesn't exist.
     *
     * @param ks
     *          The keystore to add the certificate to, may be null if it
     *          doesn't exist.
     * @param ksType
     *          The type to use if the keystore is created.
     * @param ksPath
     *          The path to the keystore if it is created.
     * @param alias
     *          The alias to store the certificate under.
     * @param pwd
     *          The password to use in saving the certificate.
     * @param certPath
     *          The path to the file containing the certificate.
     * @throws KeyStoreException
     *           If an error occurred adding the certificate to the keystore.
     */
    public final void addCertificate(KeyStore ks, String ksType, String ksPath,
        String alias, char[] pwd, String certPath) throws KeyStoreException
    {
      try
      {
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        InputStream inStream = new FileInputStream(certPath);
        if (ks == null)
        {
          ks = KeyStore.getInstance(ksType);
          ks.load(null, pwd);
        }
        // Do not support certificate replies.
        if (ks.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class))
        {
          Message msg = ERR_CERTMGR_CERT_REPLIES_INVALID.get(alias);
          throw new KeyStoreException(msg.toString());
        }
        else if (!ks.containsAlias(alias)
            || ks
                .entryInstanceOf(alias, KeyStore.TrustedCertificateEntry.class))
          trustedCert(alias, cf, ks, inStream);
        else
        {
          Message msg = ERR_CERTMGR_ALIAS_INVALID.get(alias);
          throw new KeyStoreException(msg.toString());
        }
        FileOutputStream fileOutStream = new FileOutputStream(ksPath);
        ks.store(fileOutStream, pwd);
        fileOutStream.close();
        inStream.close();
      }
      catch (Exception e)
      {
        Message msg = ERR_CERTMGR_ADD_CERT.get(alias, e.getMessage());
        throw new KeyStoreException(msg.toString());
      }
    }



    /**
     * Generate a self-signed certificate using the specified alias, dn string
     * and validity period. If the keystore does not exist, create it using the
     * specified type and path.
     *
     * @param ks
     *          The keystore to save the certificate in. May be null if it does
     *          not exist.
     * @param ksType
     *          The keystore type to use if the keystore is created.
     * @param ksPath
     *          The path to the keystore if the keystore is created.
     * @param alias
     *          The alias to store the certificate under.
     * @param pwd
     *          The password to us in saving the certificate.
     * @param dn
     *          The dn string used as the certificate subject.
     * @param validity
     *          The validity of the certificate in days.
     * @return The keystore that the self-signed certificate was stored in.
     * @throws KeyStoreException
     *           If the self-signed certificate cannot be generated.
     */
    public final KeyStore generateSelfSignedCertificate(KeyStore ks,
        String ksType, String ksPath, String alias, char[] pwd, String dn,
        int validity) throws KeyStoreException
    {
      try
      {
        if (ks == null)
        {
          ks = KeyStore.getInstance(ksType);
          ks.load(null, pwd);
        }
        else if (ks.containsAlias(alias))
        {
          Message msg = ERR_CERTMGR_ALIAS_ALREADY_EXISTS.get(alias);
          throw new KeyStoreException(msg.toString());
        }
        Object keypair = certKeyGenCons.newInstance(KEY_ALGORITHM,
            SIG_ALGORITHM);
        Object subject = X500NameCons.newInstance(dn);
        Method certAndKeyGenGenerate = certKeyGenClass.getMethod(
            GENERATE_METHOD, int.class);
        certAndKeyGenGenerate.invoke(keypair, KEY_SIZE);
        Method certAndKeyGetPrivateKey = certKeyGenClass
            .getMethod(GET_PRIVATE_KEY_METHOD);
        PrivateKey privatevKey = (PrivateKey) certAndKeyGetPrivateKey
            .invoke(keypair);
        Certificate[] certificateChain = new Certificate[1];
        Method getSelfCertificate = certKeyGenClass.getMethod(
            GET_SELFSIGNED_CERT_METHOD, X500NameClass, long.class);
        int days = validity * SEC_IN_DAY;
        certificateChain[0] = (Certificate) getSelfCertificate.invoke(keypair,
            subject, days);
        ks.setKeyEntry(alias, privatevKey, pwd, certificateChain);
        FileOutputStream fileOutStream = new FileOutputStream(ksPath);
        ks.store(fileOutStream, pwd);
        fileOutStream.close();
      }
      catch (Exception e)
      {
        Message msg = ERR_CERTMGR_GEN_SELF_SIGNED_CERT.get(alias, e
            .getMessage());
        throw new KeyStoreException(msg.toString());
      }
      return ks;
    }



    /**
     * Generate a x509 certificate from the input stream. Verification is done
     * only if it is self-signed.
     *
     * @param alias
     *          The alias to save the certificate under.
     * @param cf
     *          The x509 certificate factory.
     * @param ks
     *          The keystore to add the certificate in.
     * @param in
     *          The input stream to read the certificate from.
     * @throws KeyStoreException
     *           If the alias exists already in the keystore, if the self-signed
     *           certificate didn't verify, or the certificate could not be
     *           stored.
     */
    private void trustedCert(String alias, CertificateFactory cf, KeyStore ks,
        InputStream in) throws KeyStoreException
    {
      try
      {
        if (ks.containsAlias(alias))
        {
          Message msg = ERR_CERTMGR_ALIAS_ALREADY_EXISTS.get(alias);
          throw new KeyStoreException(msg.toString());
        }
        X509Certificate cert = (X509Certificate) cf.generateCertificate(in);
        if (isSelfSigned(cert)) cert.verify(cert.getPublicKey());
        ks.setCertificateEntry(alias, cert);
      }
      catch (Exception e)
      {
        Message msg = ERR_CERTMGR_TRUSTED_CERT.get(alias, e.getMessage());
        throw new KeyStoreException(msg.toString());
      }
    }



    /**
     * Check that the issuer and subject DNs match.
     *
     * @param cert
     *          The certificate to examine.
     * @return {@code true} if the certificate is self-signed.
     */
    private boolean isSelfSigned(X509Certificate cert)
    {
      return cert.getSubjectDN().equals(cert.getIssuerDN());
    }



    /**
     * Normalize the data in the specified buffer.
     *
     * @param buffer
     *          The buffer to normalize.
     */
    public abstract void normalize(StringBuilder buffer);



    /**
     * Calculates the usable memory which could potentially be used by the
     * application for caching objects.
     *
     * @return The usable memory which could potentially be used by the
     *         application for caching objects.
     */
    public long getUsableMemoryForCaching()
    {
      long youngGenSize = 0;
      long oldGenSize = 0;

      List<MemoryPoolMXBean> mpools = ManagementFactory.getMemoryPoolMXBeans();
      for (MemoryPoolMXBean mpool : mpools)
      {
        MemoryUsage usage = mpool.getUsage();
        if (usage != null)
        {
          String name = mpool.getName();
          if (name.equalsIgnoreCase("PS Eden Space"))
          {
            // Parallel.
            youngGenSize = usage.getMax();
          }
          else if (name.equalsIgnoreCase("PS Old Gen"))
          {
            // Parallel.
            oldGenSize = usage.getMax();
          }
          else if (name.equalsIgnoreCase("Par Eden Space"))
          {
            // CMS.
            youngGenSize = usage.getMax();
          }
          else if (name.equalsIgnoreCase("CMS Old Gen"))
          {
            // CMS.
            oldGenSize = usage.getMax();
          }
        }
      }

      if (youngGenSize > 0 && oldGenSize > youngGenSize)
      {
        // We can calculate available memory based on GC info.
        return oldGenSize - youngGenSize;
      }
      else if (oldGenSize > 0)
      {
        // Small old gen. It is going to be difficult to avoid full GCs if the
        // young gen is bigger.
        return oldGenSize * 40 / 100;
      }
      else
      {
        // Unknown GC (G1, JRocket, etc).
        Runtime runTime = Runtime.getRuntime();
        runTime.gc();
        runTime.gc();
        return (runTime.freeMemory() + (runTime.maxMemory() - runTime
            .totalMemory())) * 40 / 100;
      }
    }
  }



  // Prevent instantiation.
  private Platform()
  {
  }



  /**
   * Add the certificate in the specified path to the provided keystore;
   * creating the keystore with the provided type and path if it doesn't exist.
   *
   * @param ks
   *          The keystore to add the certificate to, may be null if it doesn't
   *          exist.
   * @param ksType
   *          The type to use if the keystore is created.
   * @param ksPath
   *          The path to the keystore if it is created.
   * @param alias
   *          The alias to store the certificate under.
   * @param pwd
   *          The password to use in saving the certificate.
   * @param certPath
   *          The path to the file containing the certificate.
   * @throws KeyStoreException
   *           If an error occurred adding the certificate to the keystore.
   */
  public static void addCertificate(KeyStore ks, String ksType, String ksPath,
      String alias, char[] pwd, String certPath) throws KeyStoreException
  {
    IMPL.addCertificate(ks, ksType, ksPath, alias, pwd, certPath);
  }



  /**
   * Delete the specified alias from the provided keystore.
   *
   * @param ks
   *          The keystore to delete the alias from.
   * @param ksPath
   *          The path to the keystore.
   * @param alias
   *          The alias to use in the request generation.
   * @param pwd
   *          The keystore password to use.
   * @throws KeyStoreException
   *           If an error occurred deleting the alias.
   */
  public static void deleteAlias(KeyStore ks, String ksPath, String alias,
      char[] pwd) throws KeyStoreException
  {
    IMPL.deleteAlias(ks, ksPath, alias, pwd);
  }



  /**
   * Generate a self-signed certificate using the specified alias, dn string and
   * validity period. If the keystore does not exist, it will be created using
   * the specified keystore type and path.
   *
   * @param ks
   *          The keystore to save the certificate in. May be null if it does
   *          not exist.
   * @param ksType
   *          The keystore type to use if the keystore is created.
   * @param ksPath
   *          The path to the keystore if the keystore is created.
   * @param alias
   *          The alias to store the certificate under.
   * @param pwd
   *          The password to us in saving the certificate.
   * @param dn
   *          The dn string used as the certificate subject.
   * @param validity
   *          The validity of the certificate in days.
   * @throws KeyStoreException
   *           If the self-signed certificate cannot be generated.
   */
  public static void generateSelfSignedCertificate(KeyStore ks, String ksType,
      String ksPath, String alias, char[] pwd, String dn, int validity)
      throws KeyStoreException
  {
    IMPL.generateSelfSignedCertificate(ks, ksType, ksPath, alias, pwd, dn,
        validity);
  }

  /**
   * Default platform class.
   */
  private static class DefaultPlatformIMPL extends PlatformIMPL
  {
    // normalize method.
    private static final Method NORMALIZE;
    // Normalized form method.
    private static final Object FORM_NFKC;

    static
    {

      Method normalize = null;
      Object formNFKC = null;
      try
      {
        Class<?> normalizer = Class.forName("java.text.Normalizer");
        Class<?> normalizerForm = Class.forName("java.text.Normalizer$Form");
        normalize = normalizer.getMethod("normalize", CharSequence.class,
            normalizerForm);
        formNFKC = normalizerForm.getField("NFKD").get(null);
      }
      catch (Exception ex)
      {
        // Do not use Normalizer. The values are already set to null.
      }
      NORMALIZE = normalize;
      FORM_NFKC = formNFKC;
    }



    @Override
    public void normalize(StringBuilder buffer)
    {
      try
      {
        String normal = (String) NORMALIZE.invoke(null, buffer, FORM_NFKC);
        buffer.replace(0, buffer.length(), normal);
      }
      catch (Exception ex)
      {
        // Don't do anything. buffer should be used.
      }
    }
  }

  /**
   * Normalize the specified buffer.
   *
   * @param buffer
   *          The buffer to normalize.
   */
  public static void normalize(StringBuilder buffer)
  {
    IMPL.normalize(buffer);
  }



  /**
   * Test if a platform java vendor property starts with the specified vendor
   * string.
   *
   * @param vendor
   *          The vendor to check for.
   * @return {@code true} if the java vendor starts with the specified vendor
   *         string.
   */
  public static boolean isVendor(String vendor)
  {
    String javaVendor = System.getProperty("java.vendor");
    return javaVendor.startsWith(vendor);
  }



  /**
   * Calculates the usable memory which could potentially be used by the
   * application for caching objects. This method <b>does not</b> look at the
   * amount of free memory, but instead tries to query the JVM's GC settings in
   * order to determine the amount of usable memory in the old generation (or
   * equivalent). More specifically, applications may also need to take into
   * account the amount of memory already in use, for example by performing the
   * following:
   *
   * <pre>
   * Runtime runTime = Runtime.getRuntime();
   * runTime.gc();
   * runTime.gc();
   * long freeCommittedMemory = runTime.freeMemory();
   * long uncommittedMemory = runTime.maxMemory() - runTime.totalMemory();
   * long freeMemory = freeCommittedMemory + uncommittedMemory;
   * </pre>
   *
   * @return The usable memory which could potentially be used by the
   *         application for caching objects.
   */
  public static long getUsableMemoryForCaching()
  {
    return IMPL.getUsableMemoryForCaching();
  }
}

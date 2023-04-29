package com.server;

import com.sun.net.httpserver.*;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

/**
 * Hello world!
 *
 */
public class Server
{
    private Server() {
    }


    public static void main( String[] args ) throws Exception {

        UserAuthenticator authchecker = new UserAuthenticator("warning");

        HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);
        final HttpContext finalContext = server.createContext("/warning", new Handler());

        //Registration context
        server.createContext("/registration", new RegistrationHandler(authchecker));

        finalContext.setAuthenticator(authchecker);

        
        char[] passphrase = args[1].toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(args[0]), passphrase);
        
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);
        
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);
        
        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        server.setHttpsConfigurator (new HttpsConfigurator(ssl) {
            public void configure (HttpsParameters params) {
    
            // get the remote address if needed
            //InetSocketAddress remote = params.getClientAddress();
    
            SSLContext c = getSSLContext();
    
            // get the default parameters
            SSLParameters sslparams = c.getDefaultSSLParameters();

            params.setSSLParameters(sslparams);
            // statement above could throw IAE if any params invalid.
            // eg. if app has a UI and parameters supplied by a user.
    
                }
            });
    
            server.setExecutor(Executors.newCachedThreadPool()); 
            server.start();
    }
}

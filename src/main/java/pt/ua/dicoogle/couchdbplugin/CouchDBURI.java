/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

/**
 *
 * @author Louis
 */
public class CouchDBURI {
    
    private URI uri;

    public CouchDBURI(String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            this.uri = null;
        }
    }

    public CouchDBURI(URI uri) {
        this.uri = uri;
    }
    
    public boolean verify(){
        if(this.uri == null)
            return false;
        String strUri = this.uri.toString();
        if(!strUri.startsWith("http://"))
            return false;
        Scanner scanner = new Scanner(strUri);
        scanner.useDelimiter(":");
        int nb = 0;
        while(scanner.hasNext()){
            scanner.next();
            nb++;
        }
        if(nb<3)
            return false;
        scanner = new Scanner(strUri);
        scanner.useDelimiter("/");
        nb = 0;
        while(scanner.hasNext()){
            scanner.next();
            nb++;
        }
        if(nb<5)
            return false;
        return true;
    }
    
    public String getFileName() {
        String str = this.uri.toString();
        int index = str.lastIndexOf("/");
        if(index == -1)
            index = str.lastIndexOf("\\");
        String result = str.substring(index + 1);
        if (result.endsWith(".dcm")) {
            result = result.substring(0, result.length() - 4);
        }
        return result;
    }

    public String getDBName() {
        if(this.uri == null)
            return null;
        String result = "";
        String strUri = this.uri.toString();
        Scanner scanner = new Scanner(strUri);
        scanner.useDelimiter("/");
        int nb = 0;
        while(scanner.hasNext()){
            String str = scanner.next();
            nb++;
            if(nb == 4)
                result = str;
        }
        return result;
    }
}

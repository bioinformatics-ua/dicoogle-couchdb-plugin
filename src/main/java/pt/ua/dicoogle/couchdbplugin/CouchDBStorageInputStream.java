/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.io.InputStream;
import java.net.URI;
import org.ektorp.AttachmentInputStream;
import org.ektorp.CouchDbConnector;
import org.ektorp.DocumentNotFoundException;
import pt.ua.dicoogle.sdk.StorageInputStream;

/**
 *
 * @author Louis
 */
public class CouchDBStorageInputStream implements StorageInputStream {

    URI uri = null;
    
    public CouchDBStorageInputStream(URI uri) {
        this.uri = uri;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public InputStream getInputStream() {
        if(this.uri == null)
            return null;
        CouchDBURI temp = new CouchDBURI(uri);
        CouchDbConnector db = CouchDBPluginSet.server.createConnector(temp.getDBName(), true);
        AttachmentInputStream attIs = null;
        try{
            attIs = db.getAttachment(temp.getFileName(), temp.getFileName()+".dcm");
        }catch(DocumentNotFoundException e){
            System.out.println(this.uri + " not found");
        }
        return attIs;
    }
    
}

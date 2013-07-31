/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pt.ua.dicoogle.couchdbplugin;

import java.net.URI;
import org.ektorp.support.CouchDbDocument;

/**
 *
 * @author Louis
 */
public class StorageDocument extends CouchDbDocument{
    
    private String type="storage";
    private URI dcmURI;
    
    public StorageDocument(){}
    public StorageDocument(URI uri){ this.dcmURI = uri; }
    public String getType(){ return this.type; }
    public void setType(String type){this.type = type; }
    public URI getDcmURI(){ return this.dcmURI; }
    public void setDcmURI(URI uri){ this.dcmURI = uri; }
}

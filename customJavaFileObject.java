
/*
 * customJavaFileObject.java
 *
 * $Id: customJavaFileObject.java,v 1.16 2013/09/16 01:35:01 sjg Exp $
 *
 * (c) Stephen Geary, Sep 2013
 *
 * Custom java compiler.
 */

import java.lang.* ;
import java.io.* ;
import java.net.* ;
import java.util.* ;
import javax.tools.*;


public class customJavaFileObject extends SimpleJavaFileObject
{
    public StringBuilder sb = null ;

    public customJavaFileObject( URI u, Kind k )
    {
        super( u, k ) ;
        
        // javacx.debug( u ) ;
        
        this.sb = javacxUtil.processToStringBuilder( u, javacx.getPreProcCmdList() ) ;
    }
    
    public CharSequence getCharContent( boolean ignoreEncodingErrors )
        throws IOException
    {
        // javacx.debug( sb ) ;
        
        return this.sb ;
    }
}



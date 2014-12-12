
/*
 * customFJFO.java
 *
 * $Id: customFJFO.java,v 1.7 2013/09/16 01:35:01 sjg Exp $
 *
 * (c) Stephen Geary, Sep 2013
 *
 * Custom Forwarding Java File Object
 */

import java.lang.* ;
import java.io.* ;
import java.net.* ;
import java.util.* ;
import javax.tools.*;


public class customFJFO<_JFO extends JavaFileObject> extends ForwardingJavaFileObject<_JFO>
{
    public StringBuilder sb = null ;

    public customFJFO( _JFO jfo )
    {
        super( jfo ) ;
        
        // javacx.debug( "Passed object is ", jfo ) ;
        
        URI u = jfo.toUri() ;
        
        if( ( jfo instanceof customJavaFileObject ) && ( ( (customJavaFileObject)jfo ).sb != null ) )
        {
            // javacx.debug( "Using already read buffer." ) ;
        
            this.sb = ( (customJavaFileObject)jfo ).sb ;
        }
        else
        {
            this.sb = javacxUtil.processToStringBuilder( u, javacx.getPreProcCmdList() ) ;
        }
    }
    
    public CharSequence getCharContent( boolean ignoreEncodingErrors )
        throws IOException
    {
        // javacx.debug( sb ) ;
        
        return this.sb ;
    }
    
    /***************************************************************************
     */
    
    /*
     * NOTE : This is important as it enables the custom file manager to
     * find and feed the correct class to the compiler proper.
     *
     * Without this the compiler would crash !
     */
    public JavaFileObject getBaseFileObject()
    {
        return this.fileObject ;
    }
}

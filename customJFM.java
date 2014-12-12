
/*
 * customJFM.java
 *
 * $Id: customJFM.java,v 1.13 2013/09/16 01:35:01 sjg Exp $
 *
 * (c) Stephen Geary, Sep 2013
 *
 * Custom JavaFileManager.
 */

import java.lang.* ;
import java.io.* ;
import java.net.* ;
import java.util.* ;
import javax.tools.*;


public class customJFM<_FM extends JavaFileManager> extends ForwardingJavaFileManager<_FM>
{
    public customJFM( _FM fm )
    {
        super( fm ) ;
    }

    /***************************************************************************
     */
    
    @Override
    public JavaFileObject getJavaFileForOutput( JavaFileManager.Location location,
                                        String className,
                                        JavaFileObject.Kind k,
                                        FileObject sibling )
                                    throws IOException
    {
        /*
        javacx.debug( location ) ;
        javacx.debug( className ) ;
        javacx.debug( k ) ;
        javacx.debug( sibling ) ;
        javacx.debug() ;
        */
        
        JavaFileObject jfo = null ;
        
        jfo = super.getJavaFileForOutput( location, className, k, sibling ) ;
        
        if( sibling instanceof customFJFO )
        {
            return jfo ;
        }
        
        if( ( sibling instanceof JavaFileObject ) && ( sibling.toString().contains( ".java" ) ) )
        {
            // javacx.debug( "Trying to replace JavaFileObject ..." ) ;

            // try replacing the file object with a customFJFO instead
            
            customFJFO<JavaFileObject> cfjfo = null ;
            
            cfjfo = new customFJFO<JavaFileObject>( (JavaFileObject)sibling ) ;
            
            if( cfjfo != null )
            {
                // javacx.debug( "New customFJFO made" ) ;
            
                jfo = super.getJavaFileForOutput( location, className, k, cfjfo ) ;
                
                // javacx.debug( jfo ) ;
            }
        }
        
        return jfo ;
    }
    
    /***************************************************************************
     */
    
    public String inferBinaryName( JavaFileManager.Location location, JavaFileObject file )
        throws IllegalStateException
    {
        String s = null ;
        
        StandardJavaFileManager sjfm = (StandardJavaFileManager)(this.fileManager) ;
    
        if( file instanceof customFJFO )
        {
            s = sjfm.inferBinaryName( location, ((customFJFO)file).getBaseFileObject() ) ;
        }
        else
        {
            s = sjfm.inferBinaryName( location, file ) ;
            
            /*
            if( file.toString().contains( ".java" ) || ( location.equals( StandardLocation.SOURCE_PATH ) ) )
            {
                javacx.debug( location ) ;
                javacx.debug( file ) ;
                
                javacx.debug( "BINARY NAME :: ", s ) ;
                
                javacx.debug( ) ;
            }
            */
        }
        
        return s ;
    }
    
    /***************************************************************************
     */
    
    public Iterable<JavaFileObject> list( JavaFileManager.Location location,
                                            String packageName,
                                            Set<JavaFileObject.Kind> kinds,
                                            boolean recurse )
        throws IOException
    {
        /*
        javacx.debug( location ) ;
        javacx.debug( packageName ) ;
        javacx.debug( kinds ) ;
        javacx.debug( recurse ) ;
        javacx.debug() ;
        */
        
        Iterable<JavaFileObject> ijfo = null ;
        
        ijfo = super.list( location, packageName, kinds, recurse ) ;
        
        // javacx.debug( "Iterable<JavaFileObject> is ", ijfo.getClass() ) ;
        
        LinkedList<JavaFileObject> ll = new LinkedList<JavaFileObject>() ;
        
        for( JavaFileObject jfo : ijfo )
        {
            if( jfo.toString().contains( ".java" ) )
            {
                // javacx.debug( jfo ) ;
                
                // try replacing the file object with a customFJFO instead
                
                customFJFO<JavaFileObject> cfjfo = null ;
                
                cfjfo = new customFJFO<JavaFileObject>( jfo ) ;
                
                ll.add( cfjfo ) ;
                
                // javacx.debug( cfjfo ) ;
            }
            else
            {
                ll.add( jfo ) ;
            }
        }
        
        // javacx.debug() ;
        
        return ll ;
    }
}



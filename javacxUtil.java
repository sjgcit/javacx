
/*
 * javacxUtil.java
 *
 * $Id: javacxUtil.java,v 1.19 2014/12/18 17:59:44 sjg Exp $
 *
 * (c) Stephen Geary, Sep 2013
 *
 * Utility class for process handling
 */

import java.lang.* ;
import java.io.* ;
import java.net.* ;
import java.util.* ;


public class javacxUtil
{
    public static StringBuilder processToStringBuilder( URI u, ArrayList<String> cmdlist )
    {
        // javacx.debug( u ) ;
        // javacx.debug( cmd ) ;
    
        StringBuilder sb = null ;
    
        // fill sb from uri source
        
        InputStream is = null ;
        
        try
        {
            URL url = u.toURL() ;
            
            is = url.openStream();
        }
        catch( MalformedURLException e )
        {
            is = null ;
        }
        catch( IOException e )
        {
            is = null ;
        }
        
        if( is == null )
        {
            javacx.debug( "Could not open stream from uri." ) ;
        
            return null ;
        }
        
        sb = javacxUtil.processToStringBuilder( u, is, cmdlist ) ;
        
        javacxUtil.closeStream( is ) ;
        
        // javacx.debug( sb ) ;
        
        return sb ;
    }
    

    
    public static StringBuilder processToStringBuilder( URI u, InputStream is, ArrayList<String> cmdlist )
    {
        /* A process that handles a sources file is going to produce
         * a relatively large output compared to the default initial
         * capacity of only 16 bytes for StringBuilder.  So we'll set
         * an initial capacity of at least 16Kb for reasonably large
         * source files.
         */
        StringBuilder sb = new StringBuilder( 16384 ) ;
        
        String tmpstr = null ;
        
        int c = 0 ;
        
        if( is == null )
        {
            return sb ;
        }
        
        // start a process
        
        ProcessBuilder pb = null ;
        
        if( ( cmdlist == null ) || ( cmdlist.size() == 0 ) )
        {
            // no commands, so forget the process
        
            c = 0 ;
            
            while( c != -1 )
            {
                c = javacxUtil.read( is ) ;
                
                if( c != -1 )
                {
                    sb.append( (char)c ) ;
                }
            }
            
            return sb ;
        }
        
        // try and process the commands
        
        InputStream     pis = null ;
        OutputStream    pos = null ;
        InputStream     pes = null ;
            
        InputStream tis = is ;
        
        String cmd = null ;
        
        int i = 0 ;
        
        for( i = 0 ; i < cmdlist.size() ; i++ )
        {
            cmd = cmdlist.get( i ) ;
            
            // javacx.debug( cmd ) ;
            
            pb = new ProcessBuilder( javacxUtil.splitCmd( cmd ) ) ;
            
            if( pb == null )
            {
                javacx.debug( "Could not create processBuilder" ) ;
            
                return null ;
            }
            
            // connect the process to our own error stream
            
            pb.redirectError( ProcessBuilder.Redirect.INHERIT ) ;
            
            Process p = null ;
            
            try
            {
                p = pb.start() ;
            }
            catch( IOException ioe )
            {
                p = null ;
            }
            
            if( p == null )
            {
                javacx.debug( "Could not start process." ) ;
            
                return null ;
            }
            
            pis = p.getInputStream() ;
            
            pos = p.getOutputStream() ;
            
            pes = p.getErrorStream() ;
            
            if( ( pis == null ) || ( pos == null ) )
            {
                javacx.debug( "Could not get process streams." ) ;
            
                javacxUtil.closeStream( pis ) ;
                javacxUtil.closeStream( pos ) ;
                
                return null ;
            }
            
            // read the input and redirect the data to the input
            // stream of the process
            
            /* Special handling of commands cpp, clang and gcc
             * where we add the #define __JAVA_FILE_ <filename>
             */
            
            if( cmd.startsWith("cpp") || cmd.startsWith("gcc") || cmd.startsWith("clang") )
            {
                String us = u.toString() ;
                
                int k = us.lastIndexOf('/') ;
                
                us = us.substring( k+1 ) ;
                
                // javacx.debug( "cpp, gcc or clang detected : prefixing __JAVA_FILE__ to "  + us + " stream." ) ;
                
                byte b[] = ( "#define __JAVA_FILE__ \"" + us + "\"\n" ).getBytes() ;
                
                try
                {
                    pos.write( b ) ;
                }
                catch( IOException ioe )
                {
                    javacx.debug( ioe ) ;
                }
            }
            
            javacxUtil.pipeAll( tis, pos ) ;
            
            javacxUtil.pipeAll( pes, System.err ) ;
            
            javacxUtil.closeStream( pos ) ;
            
            // if there are more commands then we need to
            // set tis to the correct value ( pis ).
            //
            // we also need to close InputStreams associated
            // intermediate processes.
            
            if( i > 0 )
            {
                javacxUtil.closeStream( tis ) ;
            }
            
            tis = pis ;
            
            // flush the stderr stream
            
            javacxUtil.flush( System.err ) ;
        }
        
        // now read resulting (final) output and append to stringbuilder object
        
        c = 0 ;
        
        while( c != -1 )
        {
            c = javacxUtil.read( pis ) ;
            
            if( c != -1 )
            {
                sb.append( (char)c ) ;
            }
        }
        
        if( javacx.echoProcOutToErr )
        {
            System.err.println( "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" ) ;
            System.err.println( sb ) ;
            System.err.println( "+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++" ) ;
        }
                
        // now finish up the process
        
        javacxUtil.closeStream( pis ) ;
        
        // flush the stderr stream
        
        javacxUtil.flush( System.err ) ;
        
        // javacx.debug( ) ;
        
        return sb ;
    }

    
    public static String[] splitCmd( String cmd )
    {
        return cmd.split( " " ) ;
    }
    
    public static void flush( Flushable os )
    {
        if( os == null )
        {
            return ;
        }
        
        try
        {
            os.flush() ;
        }
        catch( IOException ioe )
        {
        }
    }
    
    public static int read( InputStream is )
    {
        int c = -1 ;
    
        try
        {
            c = is.read() ;
        }
        catch( IOException ioe )
        {
            c = -1 ;
        }
        
        return c ;
    }
    
    public static void write( OutputStream os, int c )
    {
        if( c == -1 )
        {
            return ;
        }
    
        try
        {
            os.write( c ) ;
        }
        catch( IOException ioe )
        {
        }
    }

    public static void closeStream( Closeable st )
    {
        try
        {
            if( st != null )
            {
                if( st instanceof Flushable )
                {
                    javacxUtil.flush( (Flushable)st ) ;
                }
                
                st.close() ;
            }
        }
        catch( IOException ioe )
        {
        }
    }
    
    public static void pipeAll( InputStream is, OutputStream os )
    {
        int c = 0 ;
        
        while( c != -1 )
        {
            c = javacxUtil.read( is ) ;
            
            if( c != -1 )
            {
                if( javacx.echoProcOutToErr )
                {
                    javacxUtil.write( System.err, c ) ;
                }
                
                javacxUtil.write( os, c ) ;
            }
        };
    }
}



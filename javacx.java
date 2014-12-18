
/*
 * javacx.java
 *
 * $Id: javacx.java,v 1.32 2014/12/18 05:28:03 sjg Exp $
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


public class javacx
{
    private static String version = "$Revision: 1.32 $" ;

    public static boolean echoProcOutToErr = false ;

    private static long debugid = 0 ;

    private static void debugheader()
    {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[3] ;

        String linenum    = null ;
        
        linenum = String.format( "% 6d", ste.getLineNumber() ) ;
        
        String classname  = ste.getClassName() ;
        String methodname = ste.getMethodName() ;
        
        String idstr = String.format( "% 6d", debugid ) ;
    
        debugid++ ;
    
        System.out.print( idstr + " :: " + classname + " :: " + linenum + " :: " + methodname + " :: " ) ;
    }
    
    /***************************************************************************
     */
    
    public static void debug( Object... objs )
    {
        debugheader() ;
    
        for( Object obj : objs )
        {
            System.out.print( obj ) ;
        }
        
        System.out.println("") ;
    }


    /***************************************************************************
     */
    
    //private static String preProcCmd = "gcc -E -P -C -nostdinc -" ;

    private static ArrayList<String> preProcCmdList = null ;
    
    public static ArrayList<String> getPreProcCmdList()
    {
        return javacx.preProcCmdList ;
    }
    
    public static void addPreProcCmd( String cmd )
    {
        if( preProcCmdList == null )
        {
            preProcCmdList = new ArrayList<String>() ;
        }
    
        javacx.preProcCmdList.add( cmd ) ;
        
        // javacx.debug( "Adding [" + cmd + "]" ) ;
    }
    
    /*
     * parseEntry() returns the number of arguments it used
     *
     * if parseEntry() return zero or negative  it implies the argument
     * was unrecognized and the argument parse loop should give an error.
     *
     * On entry 'i' is the position of the current option to examine.
     *
     * javax options can be extended by overriding this function and
     * invoking the custom class.
     */
    
    public static LinkedList<customJavaFileObject> jfoargs = null ;
    
    public static LinkedList<String> optargs = null ;
    
    public static LinkedList<String> classargs = null ;
        

    public static int parseEntry( int i, String[] args )
    {
        String s = args[i] ;
        
        if( s.equals( "-javacx:args" ) )
        {
            for( String a : args )
            {
                javacx.debug( a ) ;
            }
            
            return 1 ;
        }
        
        if( s.equals( "-javacx:err2out-on" ) )
        {
            javacx.echoProcOutToErr = true ;
        
            return 1 ;
        }
        
        if( s.equals( "-javacx:err2out-off" ) )
        {
            javacx.echoProcOutToErr = false ;
        
            return 1 ;
        }
        
        if( s.equals( "-javacx:version" ) )
        {
            // a javax command to show the version of javax
        
            System.out.println( "javacx " + javacx.version.substring( 10, javacx.version.length()-2 ) ) ;
            
            return 1 ;
        }
        
        if( s.equals( "-javacx:cap" ) )
        {
            // use the Java version of the C Auxilary Pre-processor for the command
            
            javacx.addPreProcCmd( "java cap -" ) ;
            
            return 1 ;
        }
        
        if( s.equals( "-javacx:cpp" ) )
        {
            // use the standard cpp pre-processor for the command
            
            javacx.addPreProcCmd( "cpp -I " + System.getenv("HOME") + "/include -P -C -nostdinc -" ) ;
            
            return 1 ;
        }
        
        if( s.equals( "-javacx:gcc" ) )
        {
            // use the standard gcc compiler pre-processor stage for the command
            
            javacx.addPreProcCmd( "gcc -I " + System.getenv("HOME") + "/include -E -P -C -nostdinc -" ) ;
            
            return 1 ;
        }
        
        if( s.equals( "-javacx:cmd" ) )
        {
            // a javax option to set the preprocessor command
            // expects a quoted string for the command to follow
            
            i++ ;
            
            if( i < args.length )
            {
                // javacx.debug( "-javacx:cmd found :: [" + args[i] + "]" ) ;
            
                javacx.addPreProcCmd( args[i] ) ;
            
                return 2 ;
            }
            
            // getting here means an error
            
            return 0 ;
        }
        
        if( s.startsWith( "-javacx:" ) )
        {
            // if we get here and a javax option has not been processed then
            // it's an error
            
            return 0 ;
        }
    
        if( s.startsWith( "-" ) )
        {
            // an option
            //
            // we assume ALL options could be intended for the
            // standard compiler so we just pass them verbatim.
            
            optargs.add( s ) ;
            
            // javacx.debug( "Adding option : [" + s + "]" ) ;
            
            // some options have extra parts
            
            if(    s.equals( "-d" )
                || s.equals( "-cp" )
                || s.equals( "-classpath" )
                || s.equals( "-encoding" )
                || s.equals( "-endorseddirs" )
                || s.equals( "-extdirs" )
                || s.equals( "-processor" )
                || s.equals( "-processorpath" )
                || s.equals( "-s" )
                || s.equals( "-source" )
                || s.equals( "-sourcepath" )
                || s.equals( "-Xmaxerrs" )
                || s.equals( "-Xmaxwarns" )
                || s.equals( "-Xstdout" )
                )
            {
                i++ ;
                
                if( i < args.length )
                {
                    // javacx.debug( "Adding option : [" + args[i] + "]" ) ;
            
                    optargs.add( args[i] ) ;
                }
                
                return 2 ;
            }
            
            return 1 ;
        }
        
        if( s.endsWith( ".class" ) )
        {
            // a class
            
            classargs.add( s ) ;
        
            return 1 ;
        }
        
        if( s.startsWith( "@" ) )
        {
            // an argument list file
            
            javacx.debug( "CANNOT HANDLE argument files - ignoring." ) ;
            
            return 0 ;
        }
        
        // just a plain old file
        //
        // Note that ANY option that isn't caught will end up
        // being made a file and could error the standard compiler.
        
        File f = null ;
        
        customJavaFileObject sjfo = null ;
        
        f = new File( s ) ;
        
        sjfo = new customJavaFileObject( f.toURI(), JavaFileObject.Kind.SOURCE ) ;
        
        jfoargs.add( sjfo ) ;
        
        return 1 ;
    }
    
    public static void main( String[] args )
    {
        // javacx.debug( "javax custom compiler" ) ;
        
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler() ;
        
        // install our changes
        
        JavaFileManager fileManager = compiler.getStandardFileManager( null, null, null ) ;
        
        JavaFileManager customfileManager = new customJFM<JavaFileManager>( fileManager ) ;
        
        // organize the command line arguments for the getTask method
        
        jfoargs = new LinkedList<customJavaFileObject>() ;
        
        optargs = new LinkedList<String>() ;
        
        classargs = new LinkedList<String>() ;
        
        String s = null ;
        
        int i = 0 ;
        
        int used = 0 ;
        
        for( i = 0 ; i < args.length ; i += 1 )
        {
            used = parseEntry( i, args ) ;
            
            if( used <= 0 )
            {
                // an unrecognized option
                
                System.err.println( "Option '" + args[i] + "' was not recognized or not complete.\n" ) ;
                
                System.err.println( "Aborting compilation.\n" ) ;
                
                return ;
            }
            
            i += ( used - 1 ) ;
        }
        

        boolean result = false ;
        
            
        if(     ( jfoargs.size() == 0 )
                || ( javacx.getPreProcCmdList() == null )
                || ( javacx.getPreProcCmdList().size() == 0 )
           )
        {
            // without file arguments we cannot start the task
            // and without external commands there is no purpose
            // in other processing.
            // we need to simply start javac as a process
            
            ProcessBuilder pb = new ProcessBuilder( "javac" ) ;
            
            List<String> clist = pb.command() ;
            
            for( String t : args )
            {
                if( ! t.startsWith( "-javacx:" ) )
                {
                    clist.add( t ) ;
                }
            }
            
            if( clist.size() == 1 )
            {
                // there are no non-javax options to pass
                
                return ;
            }
            
            pb.inheritIO() ;
            
            Process p = null ;
            
            try
            {
                p = pb.start() ;
            }
            catch( IOException ioe )
            {
            }
            
            if( p != null )
            {
                int retv = -1 ;
            
                try
                {
                    retv = p.waitFor() ;
                }
                catch( InterruptedException  ie )
                {
                    javacx.debug( "Wait for completion interrupted." ) ;
                }
                
                if( retv == 0 )
                {
                    result = true ;
                }
                else
                {
                    result = false ;
                }
            }
        }
        else
        {
            JavaCompiler.CompilationTask task  = compiler.getTask( null, customfileManager, null, optargs, classargs, jfoargs ) ;
            
            
            // compile stuff
            
            result = task.call() ;
        }
        
        /*
        if( result )
        {
            javacx.debug( "\n\njavax worked.\n" ) ;
        }
        else
        {
            javacx.debug( "\n\njavax failed.\n" ) ;
        }
        */
    }
}



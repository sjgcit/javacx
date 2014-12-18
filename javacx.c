
/*
 * javacx.c
 *
 * $Id: javacx.c,v 1.26 2014/12/18 05:28:26 sjg Exp $
 *
 * (c) Stephen Geary, Sep 2013
 *
 * Invoke the javacx compiler from C which will help
 * javacx.java get quoted arguments correctly
 */

/*
#define DEBUGME
*/

#include <stdio.h>

#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <locale.h>

#if defined(_WIN32) || defined(_WIN64)
#  define nl_langinfo(CODESET)   ""
#else
#  include <langinfo.h>
#endif
    
#if defined(_WIN32) || defined(_WIN64)
#  include <process.h>
#else
  /* assume POSIX
   */
#  include <unistd.h>
#endif

#ifdef DEBUGME
#  define _DEBUGF( ... )  { fprintf( stderr, "Line % 5d\t", __LINE__ ) ; fprintf( stderr, __VA_ARGS__ ) ; }
#else
#  define _DEBUGF( ... )
#endif


#ifndef TRUE
#  define TRUE 1
#endif

#ifndef FALSE
#  define FALSE 0
#endif

#define _FREEMEM(_p)    { if( (_p) != NULL ){ free( (_p) ) ; (_p) = NULL ; } ; }


int main( int argc, char **argv )
{
    int retv = 0 ;

    int i = 0 ;
    
    char *javacxclass = "javacx" ;
    
    int *passarg = NULL ;
    
    char *javacxpath = NULL ;
    
    if( argc == 1 )
    {
        return 0 ;
    }
    
    passarg = (int *)malloc( argc * sizeof(int) ) ;
    
    if( passarg == NULL )
    {
        fprintf( stderr, "Could not allocate memory for passarg[]\n" ) ;
    
        return -1 ;
    }
    
    passarg[0] = FALSE ;
    
    int numpassedargs = 0 ;
    
    for( i = 1 ; i < argc ; i++ )
    {
        _DEBUGF( "ARG :: [%s]\n", argv[i] ) ;
        
        if( strcmp( argv[i], "-javacx:useclass" ) == 0 )
        {
            /* option tells us to use another class as the
             * compiler.
             *
             * This allows the extension of javacx relatively
             * painlessly.
             */
            
            if( i < argc-1 )
            {
                passarg[i] = FALSE ;
                
                i++ ;
            
                javacxclass = argv[i] ;
                
                passarg[i] = FALSE ;
            }
            else
            {
                fprintf( stderr, "Not enough arguments - expecting class name after '-javacx:useclass'\n" ) ;
                
                return -1 ;
            }
            
            continue ;
        }
        
        if( strcmp( argv[i], "-javacx:cp" ) == 0 )
        {
            /* option to set the path to find the javacx.class or javacx.jar
             * The default is to use the standard path ( i.e. no -cp )
             *
             * If this option is set the current directory will also be
             * used for the classpath for both jars and class files.
             */

            if( i < argc-1 )
            {
                passarg[i] = FALSE ;
                
                i++ ;
                
                passarg[i] = FALSE ;
                
                javacxpath = argv[i] ;
                
                _DEBUGF( "javacxpath = %s\n", javacxpath ) ;
            }
            else
            {
                fprintf( stderr, "Not enough arguments - expecting class name after '-javacx:useclass'\n" ) ;
                
                return -1 ;
            }
            
            continue ;
        }
        
        // an argument we pass to javacx.java
        
        numpassedargs++ ;
        
        passarg[i] = TRUE ;
    }
    
    if( numpassedargs == 0 )
    {
        /* nothing to pass to the compiler
         * so nothing to do
         */
        
        return 0 ;
    }
    
    /* passing command as first argument
     */
    
    numpassedargs++ ;
    
    /* Need to pass the javacxclass as an argument
     */
    
    numpassedargs++ ;
    
    /* have to have a NULL as a last argument for execv()
     */
     
    numpassedargs++ ;
    
    /* we need to switch this process over using execv() and
     * the command "java [-cp \"<javaclasspath>:<javaclasspath>*:.*:. <javacxclass> "
     * and the passed arguments.
     */
    
    char **newargs = NULL ;
    
    if( javacxpath != NULL )
    {
        /* note that the class path adds two arguments
         * - the "-cp" and the path itself
         */
        numpassedargs += 2 ;
    }
    
    _DEBUGF( "numpassedargs = %d\n", numpassedargs ) ;
    
    newargs = (char **)malloc( numpassedargs * sizeof( char * ) ) ;
    
    if( newargs == NULL )
    {
        fprintf( stderr, "Could not allocate memory for argument list\n" ) ;
        
        free( passarg ) ;
        
        return -1 ;
    }
    
    // fill argument array
    
    i = 0 ;
    
    newargs[ i++ ] = "java" ;
    
    char *p = NULL ;
    
    int len = 0 ;
    
    if( javacxpath != NULL )
    {
        // Note the trailing space - needed !
        len += strlen("\":/*:./*:.\"" ) ;

        len += 2*strlen(javacxpath) ;
    
        // terminating NUL char
        len++ ;
        
        p = (char *)malloc( len ) ;
        
        if( p == NULL )
        {
            free( newargs ) ;
            free( passarg ) ;
            
            fprintf( stderr, "Could not allocate memory for path string\n" ) ;
            
            return -1 ;
        }

#     if defined(_WIN32) || defined(_WIN64)
        sprintf( p, "\"%s;%s\\*;.;.\\*\"", javacxpath, javacxpath ) ;
#     else
        sprintf( p, "\"%s:%s/*:.:./*\"", javacxpath, javacxpath ) ;
#     endif
        
        newargs[ i++ ] = "-cp" ;
        
        newargs[ i++ ] = p ;
        
        _DEBUGF( "%s\n", p ) ;
    }
    

    newargs[ i++ ] = javacxclass ;
    
    int j = 0 ;
    
    for( j = 0 ; j < argc ; j++ )
    {
        if( passarg[j] )
        {
            _DEBUGF( "Adding arg[ %d ] = [%s]\n", j, argv[j] ) ;
        
            newargs[i] = argv[j] ;
            
            i++ ;
        }
    }
    
    // and the last argument must be a NULL
    
    newargs[numpassedargs-1] = NULL ;
    
    // invoke the command

#  ifdef DEBUGME
    if( i != numpassedargs-1 )
    {
        _DEBUGF( "i = %d not equal to ( numpassedargs -1 ) = %d\n", i, ( numpassedargs - 1 ) ) ;
    }
    
    for( i = 0 ; i < numpassedargs ; i++ )
    {
        _DEBUGF( "newargs[ %02d ] = [%s]\n", i, newargs[i] ) ;
    }
#  endif
    
    retv = execvp( "java", newargs ) ;

    if( retv == -1 )
    {
        _DEBUGF( "Process returned : [ %d ]\n", retv ) ;
        
        _DEBUGF( "\nError was : %d : %s\n\n", errno, strerror(errno) ) ;
    }
    
    _FREEMEM( p ) ;
    _FREEMEM( newargs ) ;
    _FREEMEM( passarg ) ;
    
    return retv ;
}

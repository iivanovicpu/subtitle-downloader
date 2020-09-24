OpenSubtitles downloader - application downloads subtitles from OpenSubtitles.org. 
After build / packaging application it could be run

java -jar jopensubs-x.x.x-jar-with-dependencies.jar 

It has implemented few download modes:

1. directory mode:
    a) downloading subtitles for all movies files which exists in directory (subtitles will be downloaded in same directory)
    example:
    --dir D:\Downloads\test
    
    or
    
    -d D:\Downloads\test
    
    b) or subtitles files could be stored in another directory:
    
    --dir D:\Downloads\test -download D:\Downloads\test\subs
    
2. download by movie title:
    a) in working directory
    -name "Forest Gump"
    -n "Forest Gump"

    b) in particular directory
    --name "Forest Gump" --t D:\Downloads\test
    --n "Forest Gump" -download D:\Downloads\test

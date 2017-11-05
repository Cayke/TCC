# Emulab Command Helper #

## Access server ##

To access remote emulab server, you need to add a ssh key on [emulab plataform](https://www.emulab.net/ssh-keys.php).
After that, start the server on panel and access from your machina using terminal. 

        $ ssh cayke@node0.caykequoruns.freestore.emulab.net
        
## Copy files to server ##

        $ scp {{file}} cayke@node0.caykequoruns.freestore.emulab.net:{{file_path_on_server}}
        
## Zip and unzip folder ##
To zip folder:

        $ tar -zcvf {{archive.tar.gz}} {{directory/}} 
        
To unzip folder:
    
        $ tar -zxvf {{archive.tar.gz}}
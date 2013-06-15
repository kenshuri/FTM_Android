FTM_Android
===========

Applications Android destinées au Frigo Time Machine

Ce répertoire contient plusieurs dossiers :
  * FrigoTimeMachine : 1ère version de l'application pour le frigo
  * FrigoTimeMachinev2 : 2ème version de l'application pour le frigo
  * BananaDetector : Application capable de détecter des bananes.


## 1. Recommendations générales
Pour commencer à coder une application Android sur Windows, je vous recommende fortement de télécharger le pack Nvidia
disponible à cette adresse : https://developer.nvidia.com/tegra-resources . Pour ce faire, il est nécessaire de créer 
un compte et de faire une demande d'inscription au Tegra Registered Developer Program (onglet MyAccount du profil). 
La validation de l'inscription ne prend que quelques heures.
Le grand intérêt de ce pack est qu'il installe la version d'OpenCV pour Android de manière simple. Pour commencer, 
je vous conseille de consulter d'abord quelques informations :
  * Le paragraphe "How to start" sur cette page contient beaucoup d'informations utiles si on veut commencer le 
    développement dans de bonnes conditions.   
  * Les "Samples" et les "Tutorials" qui sont fournis lors du téléchargement du pack Nvidia contiennent 
    aussi de nombreuses informations utiles. On les trouve dans le dossier "Samples" du dossier d'OpenCV.
  * Le journal de bord du Wiki du Frigo Time Machine (http://fablab.ensimag.fr/index.php/Frigo_Time_Machine) 
    pourra aussi vous aider.

En ce qui concerne le matériel Android nécessaire pour faire tourner l'application, il faut que ce dernier soit 
compatible avec l'API 9, ce qui est le cas si le terminal est équipée d'une version d'Android postérieure à Gingerbread
(2.3) compris. Il faut aussi que le téléphone soit équipé d'une caméra pour les prises de vue, d'un accéléromètre 
(ou bien d'une photomètre), d'une mémoire externe accessible en écriture et qu'il puisse accéder à internet 
(penser à ajouter dans le fichier manifest.xml les permissions pour utiliser toutes ces fonctionnalités).
    
## 2. Présentation des application

### L'application FrigoTimeMachine
L'application FrigoTimeMachine est développée pour faire partie d'un projet plus global. Le but est de réaliser
un système permettant à un utilisateur de consulter l’état de son frigo sur une plateforme web à l’aide de clichés 
pris régulièrement par un smartphone placé dans son frigo. 

L'objectif de notre application est donc de prendre en photo le contenu d'un frigo à chaque fois que la porte est ouverte
pour ensuite l'envoyer sur une plateforme web. Les contraintes sur les prises de vues sont d'avoir des photos à la fois
nettes du contenu du frigo et qui ne sont pas obstruées par la présence de l'utilisateur. 

Pour ce faire, le téléphone détecte tout d'abord l'ouverture du frigo grâce à l'accéloromètre (le photomètre peut 
aussi être utilisé, il serait d'ailleurs sûrement plus précis que l'accéléromètre, mais le modèle que nous utilisions 
en était dépourvu). Il lance alors une "activité" qui analyse le flux d'image reçu par la caméra du téléphone. 
Lorsqu'il détecte que l'image est stable, il prend en photo le contenu du frigo. La présence de l'utilisateur n'est donc 
pas génante puisque l'application ne considèrera pas que l'image est "stable" si l'utilisateur est présent. Il envoie 
alors la photo sur la plateforme web et continue à récupérer le flux vidéo de la caméra. Lorqu'il est 
composé uniquement d'images noires, l'application comprend que la porte du frigo est fermée et repasse dans un état
où elle attend une nouvelle ouverture de porte.

### Différenes entre les deux versions de FrigoTimeMachine

FTM_Android
===========

Applications Android destinées au Frigo Time Machine

Ce répertoire contient plusieurs dossiers :
  * FrigoTimeMachine : 1ère version de l'application pour le frigo
  * FrigoTimeMachinev2 : 2ème version de l'application pour le frigo
  * BananaDetector : Application capable de détecter des bananes.


## 1. Recommandations générales
Pour commencer à coder une application Android sur Windows, je vous recommande fortement de télécharger le pack Nvidia
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
Il faut aussi que l'application "OpenCv Manager" soit instalée sur le téléphone.
    
## 2. Présentation des applications

### L'application FrigoTimeMachine
L'application FrigoTimeMachine est développée pour faire partie d'un projet plus global. Le but est de réaliser
un système permettant à un utilisateur de consulter l’état de son frigo sur une plateforme web à l’aide de clichés 
pris régulièrement par un smartphone placé dans son frigo. 

L'objectif de notre application est donc de prendre en photo le contenu d'un frigo à chaque fois que la porte est ouverte
pour ensuite l'envoyer sur une plateforme web. Les contraintes sur les prises de vues sont d'avoir des photos à la fois
nettes du contenu du frigo et qui ne sont pas obstruées par la présence de l'utilisateur. 

Pour ce faire, le téléphone détecte tout d'abord l'ouverture du frigo grâce à l’accéléromètre (le photomètre peut 
aussi être utilisé, il serait d'ailleurs sûrement plus précis que l'accéléromètre, mais le modèle que nous utilisions 
en était dépourvu). Il lance alors une "activité" qui analyse le flux d'image reçu par la caméra du téléphone. 
Lorsqu'il détecte que l'image est stable, il prend en photo le contenu du frigo. La présence de l'utilisateur n'est donc 
pas génante puisque l'application ne considèrera pas que l'image est "stable" si l'utilisateur est présent. Il envoie 
alors la photo sur la plateforme web et continue à récupérer le flux vidéo de la caméra. Lorsqu'il est 
composé uniquement d'images noires, l'application comprend que la porte du frigo est fermée et repasse dans un état
où elle attend une nouvelle ouverture de porte.

Pour l'analyse du flux d'images, l'application utilise une bibliothèque graphique : OpenCV. OpenCV est très puissante
et permet notamment de comparer deux images entre elle et de mesurer leur "taux" de différence. Ce "taux" nous permet
ensuite de déterminer si l'image est stable. Pour ce qui du mécanisme utilisé en pratique, référez vous au code qui est 
accompagné de commentaires. Si vous utilisez eclipse et que vous voulez générer la doc, n'utilisez pas l'outil de 
génération automatique. Utilisez plutôt le fichier javadoc.xml présent à la racine du dossier de l'appli. Faîtes un clic 
droit dessus, puis "Run as" et enfin "Ant Build".

### Différences entre les deux versions de FrigoTimeMachine
Les deux versions de FrigoTimeMachine ont toutes deux le même objectif, celui de prendre une bonne photo et de l'envoyer.
Il ne faut pas considérer la version 2 comme une amélioration de la version 1, ce sont simplement deux implémentations
différentes pour répondre à un même problème.

La différence entre ces deux versions vient de la photo envoyée à la plateforme web. Dans la première version, on envoie 
une photo de qualité (bonne définition) mais dans laquelle le contenu du frigo peut-être obstruée par l'utilisateur
alors que dans la deuxième version, la définition de la photo est faible (320x240) mais le contenu du frigo est toujours
bien visible.

Dans la première version, lorsque l'on détecte que la photo est stable, on lance l'activité principale pour prendre 
une photo. Durant ce processus, l'activité correspond à OpenCV (OpenActivity) doit libérer la ressource "caméra", 
puis l'activité principale accède au téléphone pour prendre une photo. Le temps qui s'écoule durant cela n'est 
pas anecdotique et l'utilisateur a le temps de s'interposer entre l'appareil et le frigo ce qui peut aboutir à une 
photo obstruée. En revanche, dans la deuxième version, la photo est directement enregistrée dans OpenActivity,
ce qui résout le problème de l'obstruction du contenu du frigo par l'utilisateur.  Toutefois, OpenActivity utilise un
flux vidéo de faible qualité pour réaliser tous ses traitements et l'image enregistrée est donc de mauvaise qualité.

Pour le prototype final, nous avons préféré utiliser la deuxième version de l'application pour être certains que les 
photos du contenu du frigo ne seraient jamais obstruées par l'utilisateur. C'était un choix de notre part et il n'y a 
pas forcément de meilleure solution.

###Banana Detector
Dans le projet global, il y a une partie de reconnaissance d'image. En effet, le système est sensé reconnaître les aliments dans le frigo de manière automatique. Nous avions décidé à l'origine de notre projet de ne pas traiter la
partie concernant cette reconnaissance. Toutefois, par curiosité et aussi pour s'assurer de la faisabilité du projet, nous avons eu la volonté de développer une application Android capable de reconnaître un objet. C'est la vocation de Banana Detector.

Pour réaliser cette application nous avons suivi la démarche présentée dans le journal de bord du wiki que vous trouverez à ce lien : http://fablab.ensimag.fr/index.php/Frigo_Time_Machine#Journal_de_bord





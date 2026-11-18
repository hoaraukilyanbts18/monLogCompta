# monLogCompta

pour lancer l'application bureau il faudra utiliser le fichier monlogcompta sur intellij edea est mettre comme configuration 

Va dans File, Project Structure, Project.
Dans SDK, choisis Java 21. Si tu ne le vois pas, clique sur « Add SDK » puis « Download JDK », et prends la version 21.(

Lancer l’application

Ouvre l’onglet Maven sur le bord droit de l’écran.
Déplie monlogcompta, puis Plugins, puis javafx.
Double-clique sur javafx:run. La fenêtre de connexion de l’appli s’ouvre.

Créer le fichier .jar (le programme final)
Dans le même onglet Maven, déplie Lifecycle et double-clique sur package. Le fichier apparaît dans le dossier target.

Dis-moi à quelle étape tu es, ou le message d’erreur si ça bloque.

Va dans File, Project Structure, Project.
Dans SDK, choisis Java 21. Si tu ne le vois pas, clique sur « Add SDK » puis « Download JDK », et prends la version 21. ya dans sdk 25 oracle open jdk 25

Prends le 21, c’est celui que le projet attend. 

Pour avoir le 21 dans la liste

Dans Project Structure, Project, clique sur la liste SDK.
Choisis Download JDK.
Mets Version : 21 et Vendor : Eclipse Temurin, puis clique sur Download.
Une fois fini, sélectionne ce JDK 21 dans SDK.
Mets aussi Language level sur 21 juste en dessous, puis clique sur OK.


Ensuite, dans l’onglet Maven à droite, clique sur la petite icône de rechargement (deux flèches en cercle) pour que IntelliJ reprenne le projet avec Java 21. Puis lance javafx:run comme avant.


language defaut je met

Mets 21. C’est le niveau de langage qui correspond à Java 21 et à ce que demande le projet.

Dans la liste Language level, choisis quelque chose comme « 21 – Record patterns, pattern matching for switch ». Si tu vois la mention « SDK default », ça convient aussi, à condition que ton SDK soit bien le JDK 21.

Clique ensuite sur OK, puis sur l’icône de rechargement dans l’onglet Maven, et lance javafx:run.

apres

Après, tu lances l’application :

Clique sur OK pour fermer la fenêtre Project Structure.
À droite de l’écran, ouvre l’onglet Maven.
Clique sur la petite icône de rechargement (deux flèches en cercle) et attends que la barre de progression en bas se termine.
Déplie monlogcompta, puis Plugins, puis javafx.
Double-clique sur javafx:run.

La fenêtre de connexion de MonLogCompta doit s’ouvrir. Tu peux cliquer sur « Administrateur » dans les comptes de démonstration, puis sur « Se connecter ».

Si tu ne vois pas l’onglet Maven, regarde dans le menu View, puis Tool Windows, puis Maven.

Dis-moi ce qui s’affiche : la fenêtre de l’appli, ou un message d’erreur en rouge.

Déplie monlogcompta, puis Plugins, puis javafx. 


C’est dans le panneau Maven, tout à droite de la fenêtre IntelliJ. Cherche le mot Maven écrit à la verticale sur le bord droit, et clique dessus.

Tu verras alors une arborescence comme ça :

monlogcompta
  ├─ Lifecycle
  ├─ Plugins
  │    ├─ compiler
  │    ├─ javafx      ← clique sur la petite flèche pour déplier
  │    │    └─ javafx:run   ← double-clique ici
  │    └─ ...
  └─ Dependencies


Clique sur l’icône de rechargement en haut du panneau (deux flèches en cercle).
Attends que la barre en bas à droite finisse de charger.

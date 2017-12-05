#kpp.dsls2

Objectifs du parser V2 :

	- Les filtres ne sont plus une liste stackable mais un arbre
	- on ne manipule plus des "filtres" et des "routes" mais des Directives0
	- la conf et le dsl sont séparés
	- le dsl gère des déclarations
	- les directives sont paramétrables (par les infos de conf et les déclarations)
	- tout est directive (la déclaration serveur aussi)
	- le modules peuvent contribuer de nouvelles routes et directive mais aussi les infos dsls corespondantes
	
	
Le est séparé du model qu'il représente, L'objéctif est de séparer les responsabilités.
La config est modélisé dans le projet configmodel et le dsl dépand de ce projet.

Ainsi le dsl V2 devient 1 option de paramétrage du proxy et laisse la porte ouverte à :
	
	- d'autres dsls
	- config en utilisant un langage dynamique comptible jvm (groovy par exemple)
	- des clients de config graphiques
	- etc... 	
	
On obtient  : 

dsl2  ---> configModel ---> Route 

Cela permet aussi d'utiliser plusieurs modes de config simultanément. 

Par example utiliser une interface d'admin permettant de configurer des routes simples et utiliser un dsl ou un langage de script dès lors que la complexité le nécéssite.










# discovery-table-view

Metabolomic graphic client / Static HTML page generated with the [Discovery](https://github.com/p2m2/Discovery) written in scala / scalajs.

browse `table.html?endpoint=http://endpoint-metabolomics.ara.inrae.fr/peakforest/sparql`

- Displays the instances and properties of a Class: OWL entity contained in an endpoint
- Filter on property values

![](img/discovery-table-view-screenshot.png?raw=true)

# Public DockerHub 

``` 
docker push inraep2m2/table-view-discovery:0.0.1 
docker run -d -p 9909:80 --name table-view-discovery -t inraep2m2/table-view-discovery:0.0.1 
```
Try on http://localhost:9909/

# Dev Dockerized

```bash
sbt fullOptJS
chmod 644 ./target/scala-2.13/table-opt.js
docker build . -t table-view-discovery
```

```bash
docker run -d -p 9909:80 --name table-view-discovery -t table-view-discovery
```

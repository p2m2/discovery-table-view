FROM httpd:latest
COPY ./table.html /usr/local/apache2/htdocs/index.html
COPY ./target/scala-2.13/table-opt.js /usr/local/apache2/htdocs/
#CMD chmod 644 table-opt.js
#CMD chmod 755 /usr/local/apache2/htdocs/target/scala-2.13/table-opt.js.map

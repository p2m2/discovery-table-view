FROM httpd:latest

COPY ./table.html /usr/local/apache2/htdocs/index.html
COPY ./target/scala-2.13/table-opt.js /usr/local/apache2/htdocs/
COPY endpoints.js /usr/local/apache2/htdocs/

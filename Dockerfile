FROM nunopreguica/sd2526tpbase

# working directory inside docker image
WORKDIR /home/sd

ADD hibernate.cfg.xml .
ADD messages.props .

COPY target/sd2526*.jar sd2526.jar

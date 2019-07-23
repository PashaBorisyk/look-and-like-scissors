FROM ubuntu:latest AS builder

WORKDIR builder
RUN apt-get update
RUN apt -y install openjdk-8-jdk
RUN apt-get install ant

RUN apt-get -y install cmake
RUN apt-get -y install git
RUN apt-get -y install build-essential 
RUN apt-get -y install qtbase5-dev
RUN apt-get -y install qtdeclarative5-dev
RUN git clone https://github.com/Itseez/opencv.git &&\
    git clone https://github.com/Itseez/opencv_contrib.git &&\
    cd opencv && mkdir build && cd build && cmake -DBUILD_SHARED_LIBS=OFF -DWITH_QT=ON -DWITH_OPENGL=ON -DFORCE_VTK=ON -DWITH_TBB=ON -DWITH_GDAL=ON -DWITH_XINE=ON -DBUILD_EXAMPLES=ON .. && \
    make -j4 && make install && ldconfig
RUN ln /dev/null /dev/raw1394

RUN apt update

WORKDIR scala
RUN apt-get remove scala-library scala
RUN apt-get install wget
RUN wget https://www.scala-lang.org/files/archive/scala-2.12.7.deb
RUN dpkg -i scala-2.12.7.deb
RUN scala -version
RUN echo "deb https://dl.bintray.com/sbt/debian /" |  tee -a /etc/apt/sources.list.d/sbt.list
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
RUN apt-get update
RUN apt-get install sbt
RUN apt-get install unzip

COPY . .
RUN sbt dist
RUN mkdir bin
RUN unzip target/universal/look-and-like-scissors-0.1.zip -d bin


FROM ubuntu:latest

RUN apt update
RUN apt -y install openjdk-8-jdk

COPY --from=builder /builder/opencv/build/* /opencv/
COPY --from=builder /builder/scala/bin/* /app/

ENV opencv.path=/opencv/build/java/

WORKDIR app/bin/look-and-like-scissors-0.1

CMD ["ls"]


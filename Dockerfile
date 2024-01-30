FROM gcr.io/distroless/java17-debian11
COPY app/build/libs/app-all.jar app/app.jar
ENV PORT=8080
EXPOSE $PORT
WORKDIR app
CMD ["app.jar"]

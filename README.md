# SCIS v2 — Split Architecture

Two separate Maven projects — backend and frontend — sharing a clean package boundary.

## Layout

backend/
  com.scis.model   Student, AttendanceRecord, MarksRecord, StudentTask
  com.scis.db      DataManager (MongoDB — smartcampus.students)
  com.scis.ml      MLPredictor (CGPA, fail-risk, grade predictions)
  com.scis.export  CSVExporter, PDFExporter

frontend/
  com.scis.ui      Main (Java Swing — 1996-line desktop UI)

## Build
  cd backend  && mvn clean install    # installs scis-backend-2.0.0.jar locally
  cd frontend && mvn clean package    # produces target/SCIS-app.jar

## Run (requires MongoDB on localhost:27017)
  java -jar frontend/target/SCIS-app.jar

## Remote MongoDB
  Edit MONGO_URI in: backend/src/main/java/com/scis/db/DataManager.java

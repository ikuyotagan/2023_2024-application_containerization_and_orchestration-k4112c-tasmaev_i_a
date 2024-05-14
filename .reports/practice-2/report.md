University: [ITMO University](https://itmo.ru/ru/)  
Faculty: [FICT](https://fict.itmo.ru)  
Course: [Application containerization and orchestration](https://github.com/itmo-ict-faculty/application-containerization-and-orchestration)  
Year: 2023/2024  
Group: K4112c  
Author: Tasmaev Igor Aleksandrovich
Practice: practice 2  
Date of create: 13.05.2024  
Date of finished: 14.05.2024

Цель: изучить принципы работы с базами данных в контексте микросервисных приложений.

Ход работы:

1. Развернута база данных PostgreSQL в кластере minikube.

    ```yaml
   apiVersion: v1
   kind: PersistentVolume
   metadata:
     name: postgres-pv
     labels:
       type: local
   spec:
     storageClassName: manual
     capacity:
       storage: 5Gi
     accessModes:
       - ReadWriteOnce
     hostPath:
       path: "/mnt/data/postgres"
   
   ---
   
   apiVersion: v1
   kind: PersistentVolumeClaim
   metadata:
     name: postgres-pvc
   spec:
     storageClassName: manual
     accessModes:
       - ReadWriteOnce
     resources:
       requests:
         storage: 5Gi
   
   ---
   
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: postgres
   spec:
     selector:
       matchLabels:
         app: postgres
     strategy:
       type: Recreate
     template:
       metadata:
         labels:
           app: postgres
       spec:
         containers:
           - name: postgres
             image: postgres:14-alpine
             ports:
               - containerPort: 5432
             env:
               - name: POSTGRES_DB
                 value: mydatabase
               - name: POSTGRES_USER
                 value: postgres
               - name: POSTGRES_PASSWORD
                 value: secret
             volumeMounts:
               - mountPath: /var/lib/postgresql/data
                 name: postgres-storage
         volumes:
           - name: postgres-storage
             persistentVolumeClaim:
               claimName: postgres-pvc
   
   ---
   
   apiVersion: v1
   kind: Service
   metadata:
     name: postgres
   spec:
     ports:
       - port: 5432
     selector:
       app: postgres
     type: ClusterIP
    ```

Вывод: в ходе выполнения практической работы была развернута база postgresql в minikube.
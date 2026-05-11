# README

This project includes a solution for project 1 of Distributed Systems NOVA
FCT, 2025/26. This code can be used as the basis for solving the second
project, but students can use their own version.

# TO DO

LAB8
usar os comandos keytools, para todos os ourorgs servers do enunciado

keytool -ext SAN=dns:<server-name> -genkeypair -alias <server-name> -
keyalg RSA -validity 365 -keystore <keystore-filename> -storetype pkcs12

keytool -exportcert -alias <server-name> -keystore 
<keystore-file> -file <certificate-file>

keytool -importcert -file <certificate-file> -alias 
<server-name> -keystore <keystore-file>

Ex:
server-name: users-ourorg0-server
keystore-file:  users-ourorg0-server.ks
certificate-file: truststore.ks (sempre igual)

há operações(de recolha ou análise de user info ou message info) que só o servidor poderá rodar devido a partilha de secret keys que o cliente não pode fazer

LAB9

Implementar um servidor zoho

LAB11

Vamos usar kafka da biblioteca
é um servidor que usará o syncPoint singleton do lab11 para usar as threads e cenas, we got this shit
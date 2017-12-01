[<img src="https://travis-ci.org/anatse/rph.svg?branch=master"/>](https://travis-ci.org/anatse/rph/)  [![codecov](https://codecov.io/gh/anatse/rph/branch/master/graph/badge.svg)](https://codecov.io/gh/anatse/rph)

# Приложение для аптечного сайта

[Работа с программой](https://github.com/anatse/rph/wiki)

Приложение разработано на Play Scala Framework 2.6.x и Scala 2.12.x. Преставляет собой витрину аптеки (http://pharmrus24.ru).
Для загрузки товаров используется выгрузка из программы М-Аптека+ (Эскейп), выгруженная с помощью приложения [phrexp](https://github.com/anatse/prhexp).

В приложении реализованы следующие функции
1. Аутентификация, как собственная, с использованием базы данных, так и с помощью технологии OAuth, OAuth2 (библиотека [slhouette](https://www.silhouette.rocks/docs))
2. База данных товаров, загрузка, вывод на витрине


Для UI используется библиотека scala-js. На текущий момент в состав приложения включено два проекта со scala-js. Они различаются по назначению страниц. Один проект для клиентской части, другой для администрирования 

# Данные
В качестве БД используется mongoDB 3.x
Клиентская библиотека - [reactivemongo](http://reactivemongo.org/)

Для хранения изображений используется addon для heroku - cloudinary

# Система рассылки
Для рассылки email сообщений используется sendgrid

# Сборка при комитах

В проекте настроена автосборка с помощью [travis](https://travis-ci.org). Описание находится в файле .travis.yml
Для подсчета покрытия Unit тестами используется [codecoverage](https://codecov.io)

## Сборка и запуск

Перед запуском необходимо установить и запустить базу данных mongodb. Для этого можно либо установить локально либо запустить через docker.
После этого необходимо проверить параметр в конфигурационном файле /server/conf/application.conf:

```properties
mongodb.uri = "mongodb://localhost:27017/shopdb?authMode=scram-sha1"
mongodb.uri = ${?MONGODB_URI}
```

После того, как БД запущена, можно запускать само приложение. Делается это с помощью [sbt](http://www.scala-sbt.org/).

```
sbt run
```

После этого сайт доступен по ссылке [http://localhost:9000]()http://localhost:9000)

## Heroku

Приложение также сконфигурировано для запуска на [Heroku](https://heroku.com). 
Оно уже развернуто там и доступно по адресу [ФармРус](http://pharmrus24.ru)

Для развертывания используется файл Procfile. В нем есть следующая строка, которая говорит об использовании application.prod.conf в качестве конфигурации

```
web: server/target/universal/stage/bin/server -Dhttp.port=${PORT} -Dconfig.resource=application.prod.conf
```






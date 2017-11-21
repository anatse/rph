function loadRecomProducts (csrfHeader, csrfToken) {
    return function () {
        var headers = {
            'Content-type': 'application/json',
            'Accept': 'application/json'
        };

        headers[csrfHeader] = csrfToken;

        var d = $.Deferred();
        $.ajax({
            url: "/drugs/recom",
            method: "POST",
            dataType: "json",
            headers: headers
        }).done(function (response) {
            d.resolve(response.rows);
        });

        return d.promise();
    }
}

function loadProducts (csrfHeader, csrfToken) {
    return function (filter) {
        var headers = {
            'Content-type': 'application/json',
            'Accept': 'application/json'
        };

        headers[csrfHeader] = csrfToken;

        var d = $.Deferred();
        $.ajax({
            url: "/drugs/filter",
            method: "POST",
            data: JSON.stringify(filter),
            dataType: "json",
            headers: headers
        }).done(function (response) {
            d.resolve(response.rows);
        });

        return d.promise();
    }
}

function addRecommended (csrfHeader, csrfToken, drugId, onOk) {
    var headers = {
        'Content-type': 'application/json',
        'Accept': 'application/json'
    };

    console.log($)

    headers[csrfHeader] = csrfToken;

    var d = $.Deferred();
    $.ajax({
        url: "/drugs/rcmd/add?drugId=" +drugId + "&orderNum=1",
        method: "POST",
        data: JSON.stringify({
            drugId: "",
            orderNum: 1
        }),
        dataType: "json",
        headers: headers
    }).done(function (response) {
        if (response.status == 200)
            onOk();
        else
            console.log(response);
    });

    return d.promise();
}

function removeRecommended (csrfHeader, csrfToken, drugId, onOk) {
    var headers = {
        'Content-type': 'application/json',
        'Accept': 'application/json'
    };

    console.log($)

    headers[csrfHeader] = csrfToken;

    var d = $.Deferred();
    $.ajax({
        url: "/drugs/rcmd/rm?drugId=" +drugId + "&orderNum=1",
        method: "POST",
        data: JSON.stringify({
            drugId: "",
            orderNum: 1
        }),
        dataType: "json",
        headers: headers
    }).done(function (response) {
        console.log(response);
        if (response.status == 200)
            onOk();
        else
            console.log(response);
    });

    return d.promise();
}
function createProductsGrid (csrfHeader, csrfToken) {
    $("#jsGrid").jsGrid({
        height: "auto",
        width: "100%",

        sorting: true,
        paging: true,
        autoload: true,
        editing: false,
        selecting: true,

        pageIndex: 1,
        pageSize: 10,
        filtering: true,

        controller: {
            loadData: function(filter) {
                var d = $.Deferred();
                $.ajax({
                    url: "/drugs/filter",
                    method: "POST",
                    data: JSON.stringify(filter),
                    dataType: "json",
                    headers:{
                        csrfHeader: csrfToken,
                        'Content-type': 'application/json',
                        'Accept': 'application/json'
                    }
                }).done(function(response) {
                    d.resolve(response.rows);
                });

                return d.promise();
            }
        },

        fields: [
            { name: "id", type: "text", visible: false },
            { name: "drugsFullName", title: '@messages("grid.field.drugsFullName")', autosearch: true, type: "text", width: 150 },
            { name: "shortName", title: '@messages("grid.field.drugsShortName")', type: "text", width: 100 },
            { name: "ost", title: '@messages("grid.field.ost")', type: "number", autosearch: false, width: 100 },
            { name: "retailPrice", title: '@messages("grid.field.retailPrice")', type: "number", autosearch: false, width: 100 },
            {
                name: "drugImage",
                title: '@messages("grid.field.image")',
                itemTemplate: function(val, item) {
                    return $("<img>").attr("src", val).css({ width: 58, height: 30 }).on("click", function() {
                        console.log ('test');
                    });
                },
                insertTemplate: function() {
                    var insertControl = this.insertControl = $("<input>").prop("type", "file");
                    return insertControl;
                },
                insertValue: function() {
                    return this.insertControl[0].files[0];
                },
                align: "center",
                width: 120
            },
            { name: "drugGroups", title: '@messages("grid.field.groups")', type: "select", autosearch: false, width: 100 }
        ]
    });
}
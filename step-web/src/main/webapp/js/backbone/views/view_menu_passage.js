var PassageMenuView = Backbone.View.extend({
    el : function() { return $(".innerMenu li[menu-name='DISPLAY']").eq(this.model.get("passageId")); },
    events : {
        "click a[name]" : "updateModel"
    },

    initialize : function() {
        var self = this;
        this.menuOptions = this.$el.find("[name]");

        //set up menu options correctly
        var selectedOptions = this.model.get("options");
        $.each(this.menuOptions, function(i, element) {
            var item = $(element);
            if(selectedOptions.indexOf(item.attr("name")) != -1) {
                //option is present, so update it
                self._selectOption(item);
            } else {
                self._unselectMenuOption(item);
            }
        });

        //set up listeners, one in particular, which is to render the available menu options depending on the model versions
        this.listenTo(this.model, "change:version change:interlinearMode change:extraVersions change:detailLevel", this.refreshMenuOptions);

        this.refreshMenuOptions();
    },

    refreshMenuOptions : function() {
        var self = this;
        var version = this.model.get("version");
        var interlinearMode = this.model.get("interlinearMode");

        $.getSafe(BIBLE_GET_FEATURES, [version, interlinearMode], function (features) {
            //build up map of options
            $("a", self.$el).removeClass("disabled").removeAttr('title').qtip('destroy');

            for(var i = 0; i < features.removed.length; i++) {
                $("a[name='" + features.removed[i].option + "']", self.$el)
                    .addClass("disabled")
                    .attr('title', features.removed[i].explanation)
                    .qtip({ position: {my: "center right", at: "left center", viewport: $(window) }});
            }
        });
    },

    /**
     * Selects a menu option
     * @param element
     * @private
     */
    _selectOption : function(element) {
        step.menu.tickMenuItem(element);
    },

    /**
     * Unticks the menu option
     * @param element the menu element
     * @private
     */
    _unselectMenuOption : function(element) {
        $("img", element).remove();
    },

    /**
     * Gets the currently selected options in the view
     */
    getOptionString: function () {
        var self = this;
        var selectedOptions = $.map(this.menuOptions, function(anchorLink) {
            if(self.isSelected(anchorLink)) {
                return $(anchorLink).attr("name");
            }
            return undefined;
        });

        return selectedOptions;
    },

    /**
     * True, if it is selectable and has a tick
     * @param element the menu item
     * @returns {boolean}
     */
    isSelected : function(element) {
        return this.isSelectable(element) && this.isTicked(element);
    },

    /**
     * True if the option is ticked, regardless of whether it has been disabled or not
     * @param element
     */
    isTicked: function (element) {
        return $(element).has("img.selectingTick").size() != 0;
    },

    /**
     * True if the selector is not disabled
     * @param selector
     * @returns {boolean}
     */
    isSelectable : function(selector) {
        return !$(selector).hasClass("disabled");
    },

    /**
     * Updates the model
     */
    updateModel : function() {
        //get list of options
        this.model.save({
            options : this.getOptionString()
        });
    }
});
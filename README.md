# Custom Trims
## Installing custom trims.

1. Find the trim you'd like from below and click the link or navigate to the trim manually by going to the custom-trims folder.
1. Open up the Max Skill Trim plugin panel in Runelite and press the `Open Folder` button or manually navigate to `%USERPROFILE%/.runelite/max-skill-trims`.
1. Copy the trim to this folder.
1. In the `Max Skill Trim` panel press `Refresh` or restart Runelite.
1. You should now be able to select the newly installed trim.

## Creating new custom trim.

> :information_source: The templates folder contains very simple `.afdesign` and `.psd` templates so that sizing and dimensions can be ensured.

1. Create a trim using your preferred software.
1. Inside [custom-trims](/client/src/custom-trims/) decide if you want a `Designed by {name}` tag associated with the design and displayed below the trim on the website. 
    1. If you do, create a folder that is the `name` you want to display and put the trim as a file inside it. Sub-folders are allowed, but currently provide no benefit.
    1. If you do not, stick the design inside the anonymous folder.
1. Create a PR to merge your trim in.

> :warning: Filenames should be `-` seperated to allow the website to accurately format names & downloads.

> :information_source: Your filename will be the name of the downloaded file as well as the name displayed on the website. For example; `trim-43.png` would be displayed as `Trim 43` on the website and downloaded as `trim-43.png`.
